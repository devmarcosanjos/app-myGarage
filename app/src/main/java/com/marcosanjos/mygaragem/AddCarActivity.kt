package com.marcosanjos.mygaragem

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import com.marcosanjos.mygaragem.databinding.ActivityAddCarBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.model.CarLocation
import com.marcosanjos.mygaragem.service.Result
import com.marcosanjos.mygaragem.service.RetrofitClient
import com.marcosanjos.mygaragem.service.safeApiCall
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddCarBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var selectedLocation: LatLng = LatLng(0.0, 0.0)
    
    private var photoUri: Uri? = null
    private var finalImageUrl: String? = null

    private var imageFile: File? = null

    // Launcher para a Câmera (mesmo padrão do mentor)
    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            photoUri?.let { uri ->
                binding.ivAddPreview.setImageURI(uri)
                binding.tilAddImageUrl.visibility = View.GONE
            }
        }
    }

    // Launcher para permissões iniciais (localização)
    private val initialPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCurrentLocation()
        }
    }

    // Launcher para permissão da câmera (ao clicar no botão)
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permissão da câmera é necessária para tirar fotos", Toast.LENGTH_SHORT).show()
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

        setupListeners()
        checkInitialPermissions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarAdd)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAdd.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupListeners() {
        // Botão para Tirar Foto
        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Botão para Abrir campo de URL
        binding.btnAddUrl.setOnClickListener {
            binding.tilAddImageUrl.visibility = View.VISIBLE
            photoUri = null // Resetar foto se o usuário preferir URL
        }

        // Monitorar a digitação da URL para mostrar o preview
        binding.etAddImageUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = binding.etAddImageUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    finalImageUrl = url
                    Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.ic_download)
                        .error(R.drawable.ic_error)
                        .into(binding.ivAddPreview)
                }
            }
        }

        binding.btnSaveNewCar.setOnClickListener {
            validateAndSave()
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            imageFile = File.createTempFile(
                "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
                ".jpg",
                storageDir
            )
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile!!
            )
            photoUri = uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("CAMERA", "Erro ao criar arquivo de imagem", e)
            Toast.makeText(this, "Erro ao abrir a câmera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setOnMapClickListener { latLng ->
            updateMarker(latLng)
        }
        
        // Ativa botão de "minha localização" se tiver permissão
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    private fun updateMarker(latLng: LatLng) {
        selectedLocation = latLng
        googleMap?.clear()
        currentMarker = googleMap?.addMarker(
            MarkerOptions().position(latLng).title("Localização do Carro")
        )
        binding.tvAddLatLong.text = String.format(Locale.getDefault(), "Lat: %.4f, Long: %.4f", latLng.latitude, latLng.longitude)
    }

    private fun checkInitialPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            initialPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            getCurrentLocation()
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

    private fun validateAndSave() {
        val name = binding.etAddName.text.toString().trim()
        val year = binding.etAddYear.text.toString().trim()
        val licence = binding.etAddLicence.text.toString().trim()
        
        if (binding.tilAddImageUrl.isVisible) {
            val url = binding.etAddImageUrl.text.toString().trim()
            if (url.isNotEmpty()) finalImageUrl = url
        }

        var isValid = true
        if (name.isEmpty()) { binding.etAddName.error = "O nome é obrigatório"; isValid = false }
        if (year.isEmpty()) { binding.etAddYear.error = "O ano é obrigatório"; isValid = false }
        if (licence.isEmpty()) { binding.etAddLicence.error = "A placa é obrigatória"; isValid = false }

        if (selectedLocation.latitude == 0.0 && selectedLocation.longitude == 0.0) {
            Toast.makeText(this, "Por favor, selecione a localização no mapa", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) return

        // Desabilita botão para evitar múltiplos cliques
        binding.btnSaveNewCar.isEnabled = false

        if (photoUri != null) {
            uploadImageAndSave(name, year, licence)
        } else {
            saveCarToApi(name, year, licence, finalImageUrl)
        }
    }

    private fun uploadImageAndSave(name: String, year: String, licence: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = "cars/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(fileName)

        imageFile?.let { file ->
            Toast.makeText(this, "Fazendo upload da imagem...", Toast.LENGTH_SHORT).show()

            val bitmap = BitmapFactory.decodeFile(file.path)
            val baos = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            imageRef.putBytes(data)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveCarToApi(name, year, licence, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FIREBASE_STORAGE", "Erro no upload", e)
                    Toast.makeText(this, "Erro ao enviar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSaveNewCar.isEnabled = true
                }
        }
    }

    private fun saveCarToApi(name: String, year: String, licence: String, imageUrl: String?) {
        val newCar = Car(
            id = UUID.randomUUID().toString(),
            name = name,
            year = year,
            licence = licence,
            imageUrl = imageUrl,
            place = CarLocation(selectedLocation.latitude, selectedLocation.longitude)
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.addCar(newCar) }
            withContext(Dispatchers.Main) {
                binding.btnSaveNewCar.isEnabled = true
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
