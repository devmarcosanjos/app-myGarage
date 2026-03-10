package com.marcosanjos.mygaragem.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marcosanjos.mygaragem.R
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.ui.CircleTransform
import com.squareup.picasso.Picasso

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = cars[position]

        holder.tvCarName.text = car.name ?: ""
        holder.tvCarYear.text = car.year ?: ""
        holder.tvCarLicence.text = car.licence ?: ""
        
        if (!car.imageUrl.isNullOrBlank()) {
            Picasso.get()
                .load(car.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.stat_notify_error)
                .transform(CircleTransform())
                .into(holder.ivCar)
        } else {
            holder.ivCar.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    override fun getItemCount(): Int = cars.size
}