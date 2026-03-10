package com.marcosanjos.mygaragem.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Picasso.get()
            .load(url)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(android.R.drawable.stat_notify_error)
            .transform(CircleTransform())
            .into(view)
    } else {
        view.setImageResource(android.R.drawable.ic_menu_report_image)
    }
}