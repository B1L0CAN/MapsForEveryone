package com.bilocan.mapsforeveryone.api.model
 
data class TransitRequest(
    val origin: String,      // Başlangıç noktası (lat,lng formatında veya adres olarak)
    val destination: String  // Varış noktası (lat,lng formatında veya adres olarak)
) 