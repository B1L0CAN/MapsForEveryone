package com.bilocan.mapsforeveryone.api.model

import com.google.gson.annotations.SerializedName

data class LocationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("notes") val notes: String?,
    @SerializedName("user_id") val userId: String
) 