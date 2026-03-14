package com.marcosanjos.mygaragem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.marcosanjos.mygaragem.adapter.CarAdapter
import com.marcosanjos.mygaragem.database.DatabaseBuilder
import com.marcosanjos.mygaragem.database.UserLocation
import com.marcosanjos.mygaragem.databinding.ActivityMainBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.service.Result
import com.marcosanjos.mygaragem.service.RetrofitClient
import com.marcosanjos.mygaragem.service.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var allCars: List<Car> = emptyList()
    
    // Launcher para capturar o retorno da tela de detalhes ou adição
    private val detailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Ao voltar de add/edit, forçamos o filtro "Recentes" usando o ID do binding
            binding.chipGroupFilter.check(binding.chipRecent.id)
            fetchCars() 
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        try {
            DatabaseBuilder.getInstance(this)
            FavoritesManager.init(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao inicializar: ${e.message}")
        }

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        fetchCars()
        checkLocationPermissionAndRequest()

        binding.fabAddCar.setOnClickListener {
            val intent = Intent(this, AddCarActivity::class.java)
            detailsLauncher.launch(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarMain)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyCurrentFilter()
        }
    }

    private fun applyCurrentFilter() {
        val checkedId = binding.chipGroupFilter.checkedChipId
        val filtered = when (checkedId) {
            binding.chipAZ.id -> allCars.sortedBy { it.name?.lowercase() }
            binding.chipZA.id -> allCars.sortedByDescending { it.name?.lowercase() }
            binding.chipFavorites.id -> {
                val favIds = FavoritesManager.getFavoriteIds()
                allCars.filter { favIds.contains(it.id) }
            }
            // "Recentes" (chipRecent ou padrão) inverte a lista
            else -> allCars.reversed()
        }
        showCars(filtered)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.racing_red)
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchCars()
        }
    }

    private fun fetchCars() {
        binding.swipeRefreshLayout.isRefreshing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCars() }

            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false
                when (result) {
                    is Result.Success -> handleOnSuccess(result.data)
                    is Result.Error -> handleError(result.code, result.message)
                }
            }
        }
    }

    private fun checkLocationPermissionAndRequest() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseBuilder.getInstance().userLocationDao().insert(
                            UserLocation(latitude = location.latitude, longitude = location.longitude)
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Erro de localização: ${e.message}")
        }
    }

    private fun handleOnSuccess(cars: List<Car>) {
        allCars = cars
        applyCurrentFilter()
    }

    private fun showCars(cars: List<Car>) {
        binding.recyclerView.adapter = CarAdapter(
            cars = cars,
            onItemClick = { car ->
                val intent = Intent(this, CarDetailsActivity::class.java)
                intent.putExtra("car_id", car.id)
                detailsLauncher.launch(intent)
            },
            onFavoriteChanged = {
                if (binding.chipGroupFilter.checkedChipId == binding.chipFavorites.id) {
                    applyCurrentFilter()
                }
            }
        )
    }

    private fun handleError(code: Int, message: String) {
        Toast.makeText(this, "Erro ($code): $message", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                onLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
