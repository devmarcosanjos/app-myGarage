package com.marcosanjos.mygaragem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.storage.FirebaseStorage
import com.marcosanjos.mygaragem.databinding.ActivityCarDetailsBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.service.Result
import com.marcosanjos.mygaragem.service.RetrofitClient
import com.marcosanjos.mygaragem.service.safeApiCall
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailsBinding
    private var googleMap: GoogleMap? = null
    private var carLocation: LatLng? = null
    private var currentCar: Car? = null

    // Launcher para a tela de edição
    private val editCarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Se editou, recarrega os detalhes e avisa a Main que houve mudança
            currentCar?.id?.let { fetchCarDetails(it) }
            setResult(RESULT_OK) 
        }
    }

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

        val carId = intent.getStringExtra("car_id")
        if (!carId.isNullOrEmpty()) {
            fetchCarDetails(carId)
        }

        binding.btnDeleteCar.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnEditCar.setOnClickListener {
            val intent = Intent(this, EditCarActivity::class.java)
            intent.putExtra("car", currentCar) // Passa o objeto Car atual para editar
            editCarLauncher.launch(intent)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Excluir Carro")
            .setMessage("Tem certeza que deseja excluir?")
            .setPositiveButton("Sim") { _, _ ->
                currentCar?.id?.let { deleteCar(it) }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun deleteCar(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(id) }
            withContext(Dispatchers.Main) {
                if (result is Result.Success) {
                    deleteImageFromFirebase(currentCar?.imageUrl)
                    Toast.makeText(this@CarDetailsActivity, "Carro excluído!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun deleteImageFromFirebase(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return
        try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete()
        } catch (e: Exception) {
            // Imagem pode ser uma URL externa (não do Firebase), ignora silenciosamente
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
                        currentCar = car
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
        Picasso.get()
            .load(car.imageUrl)
            .placeholder(R.drawable.ic_download)
            .error(R.drawable.ic_error)
            .into(binding.ivDetailCar)
    }
}