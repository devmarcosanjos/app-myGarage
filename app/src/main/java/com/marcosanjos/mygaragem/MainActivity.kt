package com.marcosanjos.mygaragem

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
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

        setupRecyclerView()
        setupSwipeRefresh()
        fetchCars()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("MainActivity", "Atualizando lista...")
            fetchCars()
        }
    }

    private fun fetchCars() {
        binding.swipeRefreshLayout.isRefreshing = true
        Log.d("MainActivity", "Buscando carros da API...")
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCars() }

            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false
                when (result) {
                    is Result.Success -> {
                        Log.d("MainActivity", "Sucesso! Recebidos ${result.data.size} carros")
                        handleOnSuccess(result.data)
                    }
                    is Result.Error -> {
                        Log.e("MainActivity", "Erro na API: ${result.message} (Código: ${result.code})")
                        handleError(result.code, result.message)
                    }
                }
            }
        }
    }

    private fun handleOnSuccess(cars: List<Car>) {
        if (cars.isEmpty()) {
            Log.w("MainActivity", "A lista de carros veio vazia.")
            Toast.makeText(this, "Nenhum carro encontrado", Toast.LENGTH_SHORT).show()
        }
        val adapter = CarAdapter(cars)
        binding.recyclerView.adapter = adapter
    }

    private fun handleError(code: Int, message: String) {
        Toast.makeText(this, "Erro ($code): $message", Toast.LENGTH_SHORT).show()
    }
}