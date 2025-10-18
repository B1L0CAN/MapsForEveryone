package com.bilocan.mapsforeveryone.api.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Any = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("token") val token: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("user_id") val user_id: String? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("data") val data: JsonElement? = null,
    // Herhangi bir alan ismi için catch-all:
    val additionalProperties: Map<String, Any?> = emptyMap()
) 