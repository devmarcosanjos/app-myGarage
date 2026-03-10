package com.marcosanjos.mygaragem

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class CarDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val carId = intent.getStringExtra("car_id")
        Log.d("CarDetails", "ID recebido da Intent: $carId")

        if (!carId.isNullOrEmpty()) {
            fetchCarDetails(carId)
        } else {
            Toast.makeText(this, "ID do carro é nulo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCarDetails(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("CarDetails", "Buscando detalhes para o ID: $id")
            val result = safeApiCall { RetrofitClient.apiService.getCarById(id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        // A API retorna um CarDetailResponse que contém o campo 'value' (que é o Car)
                        val response = result.data
                        val car = response.value
                        if (car != null) {
                            Log.d("CarDetails", "Carro encontrado: ${car.name}")
                            setupUI(car)
                        } else {
                            Log.w("CarDetails", "Objeto 'value' nulo no JSON")
                            Toast.makeText(this@CarDetailsActivity, "Dados do carro não encontrados", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is Result.Error -> {
                        Log.e("CarDetails", "Erro na API: ${result.message}")
                        Toast.makeText(this@CarDetailsActivity, "Erro ao carregar: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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