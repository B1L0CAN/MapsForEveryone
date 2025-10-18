package com.bilocan.mapsforeveryone.api

import com.bilocan.mapsforeveryone.api.model.AuthRequest
import com.bilocan.mapsforeveryone.api.model.AuthResponse
import com.bilocan.mapsforeveryone.api.model.LoginRequest
import com.bilocan.mapsforeveryone.api.model.LoginResponse
import com.bilocan.mapsforeveryone.api.model.RegisterRequest
import com.bilocan.mapsforeveryone.api.model.RegisterResponse
import com.bilocan.mapsforeveryone.api.model.LocationRequest
import com.bilocan.mapsforeveryone.api.model.LocationResponse
import com.bilocan.mapsforeveryone.api.model.LocationListResponse
import com.bilocan.mapsforeveryone.api.model.TransitRequest
import com.bilocan.mapsforeveryone.api.model.TransitResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // Auth endpointleri
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    // Eski metod imzaları - geriye dönük uyumluluk için
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/login")
    suspend fun loginLegacy(@Body request: LoginRequest): Response<LoginResponse>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/register")
    suspend fun registerLegacy(@Body request: RegisterRequest): Response<RegisterResponse>
    
    // Alternatif olarak ResponseBody tipinde yanıt dönen metodlar
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/login-raw")
    suspend fun loginRaw(@Body request: AuthRequest): Response<ResponseBody>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/register")
    suspend fun registerRaw(@Body request: RegisterRequest): Response<ResponseBody>
    
    // Favori yerler için API metodları
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @GET("api/locations")
    suspend fun getLocations(): Response<List<LocationResponse>>
    
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("api/locations")
    suspend fun createLocation(@Body location: LocationRequest): Response<LocationResponse>
    
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @PUT("api/locations/{id}")
    suspend fun updateLocation(@Path("id") id: String, @Body location: LocationRequest): Response<LocationResponse>
    
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @DELETE("api/locations/{name}")
    suspend fun deleteLocation(@Path("name") name: String): Response<ResponseBody>
    
    // Transit API endpointi
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @GET("api/transit")
    suspend fun getTransitRoute(@Query("origin") origin: String, @Query("destination") destination: String): Response<TransitResponse>
} 