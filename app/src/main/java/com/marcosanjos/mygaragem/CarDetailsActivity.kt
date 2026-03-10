package com.marcosanjos.mygaragem

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.marcosanjos.mygaragem.databinding.ActivityCarDetailsBinding
import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.ui.CircleTransform
import com.squareup.picasso.Picasso

class CarDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recupera o carro enviado pela Intent
        val car = intent.getSerializableExtra("car") as? Car

        car?.let {
            binding.tvDetailName.text = it.name
            binding.tvDetailYear.text = "Ano: ${it.year}"
            binding.tvDetailLicence.text = "Placa: ${it.licence}"
            
            Picasso.get()
                .load(it.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.stat_notify_error)
                .transform(CircleTransform())
                .into(binding.ivDetailCar)
        }
    }
}