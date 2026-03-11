package com.marcosanjos.mygaragem

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.marcosanjos.mygaragem.databinding.ActivityCarDetailsBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.service.Result
import com.marcosanjos.mygaragem.service.RetrofitClient
import com.marcosanjos.mygaragem.service.safeApiCall
import com.marcosanjos.mygaragem.ui.CircleTransform
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailsBinding
    private var googleMap: GoogleMap? = null
    private var carLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCarDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        // Inicializa o fragmento do mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val carId = intent.getStringExtra("car_id")
        if (!carId.isNullOrEmpty()) {
            fetchCarDetails(carId)
        } else {
            Toast.makeText(this, "ID do carro é nulo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        updateMapLocation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.toolbarDetails.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun fetchCarDetails(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCarById(id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        val car = result.data.value
                        if (car != null) {
                            setupUI(car)
                            // Salva a localização para o mapa
                            val lat = car.place?.lat
                            val long = car.place?.long
                            if (lat != null && long != null) {
                                carLocation = LatLng(lat, long)
                                updateMapLocation()
                            }
                        }
                    }
                    is Result.Error -> {
                        Toast.makeText(this@CarDetailsActivity, "Erro: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateMapLocation() {
        val map = googleMap ?: return
        val location = carLocation ?: return

        map.addMarker(MarkerOptions().position(location).title("Localização do Carro"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    private fun setupUI(car: Car) {
        binding.tvDetailName.text = car.name ?: "Sem nome"
        binding.tvDetailYear.text = "Ano: ${car.year ?: "N/A"}"
        binding.tvDetailLicence.text = "Placa: ${car.licence ?: "N/A"}"
        
        Picasso.get()
            .load(car.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(android.R.drawable.stat_notify_error)
            .transform(CircleTransform())
            .into(binding.ivDetailCar)
    }
}