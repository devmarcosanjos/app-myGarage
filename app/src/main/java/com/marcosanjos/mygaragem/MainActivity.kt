package com.marcosanjos.mygaragem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.marcosanjos.mygaragem.adapter.CarAdapter
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
    
    // O ActivityResultLauncher DEVE ser inicializado aqui ou no onCreate
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

        // Inicializa o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRecyclerView()
        setupSwipeRefresh()
        fetchCars()
        
        // Tenta obter a localização ao iniciar (pedirá permissão se necessário)
        checkLocationPermissionAndRequest()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSwipeRefresh() {
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
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            else -> {
                locationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun getLastLocation() {
        // Verificação dupla de segurança para o compilador
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("MainActivity", "Latitude: $latitude, Longitude: $longitude")
            } else {
                Log.d("MainActivity", "Localização está nula (pode estar desativada no dispositivo)")
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao obter a localização", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleOnSuccess(cars: List<Car>) {
        if (cars.isEmpty()) {
            Toast.makeText(this, "Nenhum carro encontrado", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerView.adapter = CarAdapter(cars) { car -> 
            Log.d("MainActivity", "Clicou no carro id: ${car.id}")
            
            val intent = Intent(this, CarDetailsActivity::class.java)
            intent.putExtra("car_id", car.id)
            startActivity(intent)
        }
    }

    private fun handleError(code: Int, message: String) {
        Toast.makeText(this, "Erro ($code): $message", Toast.LENGTH_SHORT).show()
    }
}
