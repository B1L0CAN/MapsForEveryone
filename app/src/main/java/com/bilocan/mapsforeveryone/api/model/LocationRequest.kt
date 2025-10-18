package com.bilocan.mapsforeveryone.api.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class LocationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("timestamp") val timestamp: String = createTimestamp()
) {
    companion object {
        // MS SQL Server'ın doğru biçimlendirilmiş bir timestamp değeri için
        private fun createTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC") // UTC zaman dilimini kullan
            return sdf.format(Date())
        }
    }
    
    override fun toString(): String {
        return "LocationRequest(name='$name', address='$address', lat=$latitude, lng=$longitude, notes='$notes', timestamp='$timestamp')"
    }
} 