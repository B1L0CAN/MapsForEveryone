package com.bilocan.mapsforeveryone.api

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private val BASE_URL = BuildConfig.BACKEND_URL
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun clearClientState() {
        Log.d(TAG, "API istemci durumu temizleniyor...")
    }
    
    // Token interceptor
    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val prefs = appContext?.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val token = prefs?.getString("user_token", "") ?: ""
            
            if (BuildConfig.DEBUG_LOGS) {
                Log.d(TAG, "Token interceptor çalışıyor. Token: ${token.take(5)}...")
                Log.d(TAG, "İstek URL: ${originalRequest.url}")
                Log.d(TAG, "İstek metodu: ${originalRequest.method}")
            }
            
            val newRequest = if (token.isNotEmpty()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            
            try {
                val response = chain.proceed(newRequest)
                if (BuildConfig.DEBUG_LOGS) {
                    Log.d(TAG, "Sunucu yanıt kodu: ${response.code}")
                }
                return response
            } catch (e: Exception) {
                if (BuildConfig.DEBUG_LOGS) {
                    Log.e(TAG, "İstek sırasında hata: ${e.message}", e)
                }
                throw e
            }
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor())
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
} 