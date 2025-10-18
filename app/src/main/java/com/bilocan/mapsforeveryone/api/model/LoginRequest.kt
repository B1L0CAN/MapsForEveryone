package com.bilocan.mapsforeveryone.api.model

import com.google.gson.annotations.SerializedName
 
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String? = null // Bazı API'ler email yerine username kullanır
) 