package com.marcosanjos.mygaragem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
    private var currentCarId: String? = null

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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        currentCarId = intent.getStringExtra("car_id")
        if (!currentCarId.isNullOrEmpty()) {
            fetchCarDetails(currentCarId!!)
        }

        binding.btnDeleteCar.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Excluir Carro")
            .setMessage("Tem certeza que deseja excluir este carro?")
            .setPositiveButton("Sim") { _, _ ->
                currentCarId?.let { deleteCar(it) }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun deleteCar(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@CarDetailsActivity, "Carro excluído!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK) // Avisa a MainActivity que houve mudança
                        finish() // Volta para a tela anterior
                    }
                    is Result.Error -> {
                        Toast.makeText(this@CarDetailsActivity, "Erro: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        updateMapLocation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarDetails.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun fetchCarDetails(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCarById(id) }
            withContext(Dispatchers.Main) {
                if (result is Result.Success) {
                    val car = result.data.value
                    if (car != null) {
                        setupUI(car)
                        car.place?.let {
                            if (it.lat != null && it.long != null) {
                                carLocation = LatLng(it.lat, it.long)
                                updateMapLocation()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateMapLocation() {
        val map = googleMap ?: return
        val location = carLocation ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(location).title("Localização"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    private fun setupUI(car: Car) {
        binding.tvDetailName.text = car.name
        binding.tvDetailYear.text = "Ano: ${car.year}"
        binding.tvDetailLicence.text = "Placa: ${car.licence}"
        Picasso.get().load(car.imageUrl).transform(CircleTransform()).into(binding.ivDetailCar)
    }
}