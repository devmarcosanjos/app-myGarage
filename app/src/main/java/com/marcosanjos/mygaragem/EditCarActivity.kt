package com.marcosanjos.mygaragem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.storage.FirebaseStorage
import com.marcosanjos.mygaragem.databinding.ActivityEditCarBinding
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

class EditCarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditCarBinding
    private var carId: String? = null
    
    private var photoUri: Uri? = null
    private var imageFile: File? = null
    private var currentImageUrl: String? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            photoUri?.let { uri ->
                binding.ivEditPreview.setImageURI(uri)
                binding.etImageUrl.setText("")
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permissão da câmera necessária", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        val car = intent.getSerializableExtra("car") as? Car
        if (car != null) {
            carId = car.id
            fillFields(car)
        }

        binding.btnEditTakePhoto.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        binding.btnSaveCar.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarEdit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarEdit.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun fillFields(car: Car) {
        binding.etName.setText(car.name)
        binding.etYear.setText(car.year)
        binding.etLicence.setText(car.licence)
        binding.etImageUrl.setText(car.imageUrl)
        binding.etLat.setText(car.place?.lat?.toString())
        binding.etLong.setText(car.place?.long?.toString())
        
        currentImageUrl = car.imageUrl
        if (!car.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(car.imageUrl)
                .placeholder(R.drawable.ic_download)
                .error(R.drawable.ic_error)
                .into(binding.ivEditPreview)
        }
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            imageFile = File.createTempFile(
                "EDIT_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
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
            Toast.makeText(this, "Erro ao abrir câmera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateAndSave() {
        val id = carId ?: return
        val name = binding.etName.text.toString().trim()
        val year = binding.etYear.text.toString().trim()
        val licence = binding.etLicence.text.toString().trim()
        val manualUrl = binding.etImageUrl.text.toString().trim()

        if (name.isEmpty() || year.isEmpty() || licence.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveCar.isEnabled = false

        if (photoUri != null) {
            // Se tirou foto nova, faz upload primeiro
            uploadImageAndSave(id, name, year, licence)
        } else {
            // Se não tirou foto, usa a URL manual ou a antiga
            val finalUrl = if (manualUrl.isNotEmpty()) manualUrl else currentImageUrl
            updateCarInApi(id, name, year, licence, finalUrl)
        }
    }

    private fun uploadImageAndSave(id: String, name: String, year: String, licence: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("cars/${UUID.randomUUID()}.jpg")

        imageFile?.let { file ->
            Toast.makeText(this, "Atualizando imagem...", Toast.LENGTH_SHORT).show()

            val bitmap = BitmapFactory.decodeFile(file.path)
            val baos = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            imageRef.putBytes(data)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updateCarInApi(id, name, year, licence, downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    binding.btnSaveCar.isEnabled = true
                    Toast.makeText(this, "Erro no upload da foto", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateCarInApi(id: String, name: String, year: String, licence: String, imageUrl: String?) {
        val updatedCar = Car(
            id = id,
            name = name,
            year = year,
            licence = licence,
            imageUrl = imageUrl,
            place = CarLocation(
                lat = binding.etLat.text.toString().toDoubleOrNull(),
                long = binding.etLong.text.toString().toDoubleOrNull()
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.updateCar(id, updatedCar) }
            withContext(Dispatchers.Main) {
                binding.btnSaveCar.isEnabled = true
                if (result is Result.Success) {
                    Toast.makeText(this@EditCarActivity, "Alterações salvas!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditCarActivity, "Erro ao salvar no servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
