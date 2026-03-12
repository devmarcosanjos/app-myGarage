package com.marcosanjos.mygaragem

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.marcosanjos.mygaragem.databinding.ActivityEditCarBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.model.CarLocation
import com.marcosanjos.mygaragem.service.Result
import com.marcosanjos.mygaragem.service.RetrofitClient
import com.marcosanjos.mygaragem.service.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditCarBinding
    private var carId: String? = null

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

        binding.btnSaveCar.setOnClickListener {
            saveChanges()
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
    }

    private fun saveChanges() {
        val id = carId ?: return
        
        val updatedCar = Car(
            id = id,
            name = binding.etName.text.toString(),
            year = binding.etYear.text.toString(),
            licence = binding.etLicence.text.toString(),
            imageUrl = binding.etImageUrl.text.toString(),
            place = CarLocation(
                lat = binding.etLat.text.toString().toDoubleOrNull(),
                long = binding.etLong.text.toString().toDoubleOrNull()
            )
        )

        Log.d("EDIT_CAR", "Enviando atualização para o ID: $id")

        CoroutineScope(Dispatchers.IO).launch {
            // Chamada direta para testar a persistência
            val result = safeApiCall { RetrofitClient.apiService.updateCar(id, updatedCar) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Log.d("EDIT_CAR", "Sucesso ao atualizar: ${result.data}")
                        Toast.makeText(this@EditCarActivity, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    is Result.Error -> {
                        Log.e("EDIT_CAR", "Erro ao salvar: ${result.message} (Código: ${result.code})")
                        Toast.makeText(this@EditCarActivity, "Erro ao salvar no servidor: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}