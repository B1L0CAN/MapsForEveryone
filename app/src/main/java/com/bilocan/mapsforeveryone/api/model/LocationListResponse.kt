package com.bilocan.mapsforeveryone.api.model

import com.google.gson.annotations.SerializedName
 
data class LocationListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("locations") val locations: List<LocationResponse> = emptyList()
) 