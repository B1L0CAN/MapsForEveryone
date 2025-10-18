package com.bilocan.mapsforeveryone.data

interface FavoriteRepository {
    fun getAllFavorites(): List<FavoriteLocation>
    fun getFavoriteById(id: String): FavoriteLocation?
    fun addFavorite(favorite: FavoriteLocation)
    fun updateFavorite(favorite: FavoriteLocation)
    fun deleteFavorite(favorite: FavoriteLocation)
} 