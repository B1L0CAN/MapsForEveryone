package com.bilocan.mapsforeveryone.data

import android.util.Log

data class FavoriteLocation(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = ""
) {
    override fun toString(): String {
        return "FavoriteLocation(id='$id', name='$name', address='$address', lat=$latitude, lng=$longitude, notes='$notes')"
    }
    
    init {
        // Konsola yazdırma
        Log.d("FavoriteLocation", "Created: $this")
    }
} 