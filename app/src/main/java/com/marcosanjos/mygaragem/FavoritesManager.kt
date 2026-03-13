package com.marcosanjos.mygaragem

import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {

    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_ids"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFavorite(carId: String): Boolean {
        return getFavoriteIds().contains(carId)
    }

    fun toggleFavorite(carId: String): Boolean {
        val favorites = getFavoriteIds().toMutableSet()
        val isFav = if (favorites.contains(carId)) {
            favorites.remove(carId)
            false
        } else {
            favorites.add(carId)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        return isFav
    }

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }
}
