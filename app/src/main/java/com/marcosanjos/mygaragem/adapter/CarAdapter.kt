package com.marcosanjos.mygaragem.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.marcosanjos.mygaragem.R
import com.marcosanjos.mygaragem.model.Car

class CarAdapter(
    private val cars: List<Car>
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCar: ImageView = view.findViewById(R.id.ivCar)
        val tvCarName: TextView = view.findViewById(R.id.tvCarName)
        val tvCarYear: TextView = view.findViewById(R.id.tvCarYear)
        val tvCarLicence: TextView = view.findViewById(R.id.tvCarLicence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_car, parent, false)
            CarViewHolder(view)
        } catch (e: Exception) {
            Log.e("CarAdapter", "Erro ao inflar layout: ${e.message}")
            throw e
        }
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = cars[position]
        Log.d("CarAdapter", "Binding carro: ${car.name}")

        holder.tvCarName.text = car.name ?: "Sem nome"
        holder.tvCarYear.text = "Ano: ${car.year ?: "N/A"}"
        holder.tvCarLicence.text = "Placa: ${car.licence ?: "N/A"}"
        
        holder.ivCar.load(car.imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
            error(android.R.drawable.stat_notify_error)
        }
    }

    override fun getItemCount(): Int = cars.size
}