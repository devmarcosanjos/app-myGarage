package com.marcosanjos.mygaragem

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.marcosanjos.mygaragem.databinding.ActivityAddCarBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.model.CarLocation
import com.marcosanjos.mygaragem.service.Result
import com.marcosanjos.mygaragem.service.RetrofitClient
import com.marcosanjos.mygaragem.service.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AddCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddCarBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var selectedLocation: LatLng = LatLng(0.0, 0.0)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.addMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()

        binding.btnSaveNewCar.setOnClickListener {
            saveCar()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarAdd)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAdd.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Listener para clique no mapa (mudar o pin)
        googleMap?.setOnMapClickListener { latLng ->
            updateMarker(latLng)
        }
    }

    private fun updateMarker(latLng: LatLng) {
        selectedLocation = latLng
        googleMap?.clear()
        currentMarker = googleMap?.addMarker(
            MarkerOptions().position(latLng).title("Localização do Carro").draggable(true)
        )
        binding.tvAddLatLong.text = "Lat: ${String.format("%.4f", latLng.latitude)}, Long: ${String.format("%.4f", latLng.longitude)}"
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                updateMarker(userLatLng)
            }
        }
    }

    private fun saveCar() {
        val name = binding.etAddName.text.toString().trim()
        val year = binding.etAddYear.text.toString().trim()
        val licence = binding.etAddLicence.text.toString().trim()
        val imageUrl = binding.etAddImageUrl.text.toString().trim()

        var isValid = true

        if (name.isEmpty()) {
            binding.etAddName.error = "O nome é obrigatório"
            isValid = false
        }

        if (year.isEmpty()) {
            binding.etAddYear.error = "O ano é obrigatório"
            isValid = false
        }

        if (licence.isEmpty()) {
            binding.etAddLicence.error = "A placa é obrigatória"
            isValid = false
        }

        // Validação básica de localização: se ainda for 0,0 e o usuário não alterou, 
        // talvez queira avisar, mas como você disse que ele pode alterar, 
        // vamos apenas garantir os campos de texto primeiro.
        if (selectedLocation.latitude == 0.0 && selectedLocation.longitude == 0.0) {
            Toast.makeText(this, "Por favor, selecione a localização no mapa", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) return

        val newCar = Car(
            id = UUID.randomUUID().toString(),
            name = name,
            year = year,
            licence = licence,
            imageUrl = if (imageUrl.isNotEmpty()) imageUrl else null,
            place = CarLocation(selectedLocation.latitude, selectedLocation.longitude)
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.addCar(newCar) }
            withContext(Dispatchers.Main) {
                if (result is Result.Success) {
                    Toast.makeText(this@AddCarActivity, "Carro adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddCarActivity, "Erro ao salvar no servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
