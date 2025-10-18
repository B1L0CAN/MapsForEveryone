package com.bilocan.mapsforeveryone.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesFavoriteRepository(
    private val context: Context
) : FavoriteRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun getAllFavorites(): List<FavoriteLocation> {
        val json = prefs.getString(KEY_FAVORITES, "[]")
        val type = object : TypeToken<List<FavoriteLocation>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    override fun getFavoriteById(id: String): FavoriteLocation? {
        return getAllFavorites().find { it.id == id }
    }

    override fun addFavorite(favorite: FavoriteLocation) {
        val favorites = getAllFavorites().toMutableList()
        favorites.add(favorite)
        saveFavorites(favorites)
    }

    override fun updateFavorite(favorite: FavoriteLocation) {
        val favorites = getAllFavorites().toMutableList()
        val index = favorites.indexOfFirst { it.id == favorite.id }
        if (index != -1) {
            favorites[index] = favorite
            saveFavorites(favorites)
        }
    }

    override fun deleteFavorite(favorite: FavoriteLocation) {
        val favorites = getAllFavorites().toMutableList()
        favorites.removeIf { it.id == favorite.id }
        saveFavorites(favorites)
    }

    private fun saveFavorites(favorites: List<FavoriteLocation>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString(KEY_FAVORITES, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "favorites_prefs"
        private const val KEY_FAVORITES = "favorites"
    }
} 