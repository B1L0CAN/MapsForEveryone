package com.bilocan.mapsforeveryone.api.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token") val token: String = "",
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("success") val success: Boolean = true
) 