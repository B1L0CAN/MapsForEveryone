package com.bilocan.mapsforeveryone.api

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    
    // BASE_URL'i ana sunucu yoluna ayarlıyoruz, auth ve api yolları ayrı ayrı düzenleniyor
    // Android emülatörde localhost yerine 10.0.2.2 kullanılmalı
    private val BASE_URL = BuildConfig.BACKEND_URL
    
    // Context referansı
    private var appContext: Context? = null
    
    // Context set etmek için
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    // Client state'i temizler
    fun clearClientState() {
        // Şu anda bu fonksiyon boş, gerektiğinde istemci state'ini temizleme özellikleri eklenebilir
        Log.d(TAG, "API istemci durumu temizleniyor...")
    }
    
    // Daha detaylı log için
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Daha detaylı URL ve yanıt loglaması ekliyoruz
    private val urlLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        
        // Tam URL'yi log'la
        val fullUrl = request.url.toString()
        Log.d(TAG, "API İsteği Detayları:")
        Log.d(TAG, "Method: ${request.method}")
        Log.d(TAG, "URL: $fullUrl")
        
        // Headers'ı tek tek logla
        val headersBuilder = StringBuilder()
        request.headers.forEach { (name, value) ->
            headersBuilder.append("$name: $value\n")
        }
        Log.d(TAG, "Headers:\n$headersBuilder")
        
        // İsteği yap
        try {
            val startTime = System.currentTimeMillis()
            val response = chain.proceed(request)
            val endTime = System.currentTimeMillis()
            
            Log.d(TAG, "API Yanıt Detayları:")
            Log.d(TAG, "Status: ${response.code} ${response.message}")
            Log.d(TAG, "Süre: ${endTime - startTime}ms")
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "İstek sırasında hata: ${e.message}", e)
            throw e
        }
    }
    
    // İstek header'larını ekleyen bir interceptor
    private val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val url = original.url.toString()
        
        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        
        // Eğer auth endpoint'lerine istek yapılıyorsa token ekleme
        if (!url.contains("/auth/login") && !url.contains("/auth/register")) {
            // Eğer token varsa Authorization header'ı ekle
            appContext?.let { context ->
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val token = prefs.getString("user_token", "")
                if (!token.isNullOrEmpty()) {
                    Log.d(TAG, "Authorization token ekleniyor: $token")
                    requestBuilder.header("Authorization", "Bearer $token")
                } else {
                    Log.d(TAG, "Token bulunamadı, Authorization header'ı eklenmiyor")
                }
            }
        } else {
            Log.d(TAG, "Auth endpoint'i için token eklenmiyor: $url")
        }
        
        val request = requestBuilder.method(original.method, original.body).build()
        chain.proceed(request)
    }
    
    // HTTP hata durumlarını daha iyi işleyen interceptor
    private val errorInterceptor = Interceptor { chain ->
        try {
            val request = chain.request()
            val response = chain.proceed(request)
            
            // HTTP hata kodlarını loglama
            when (response.code) {
                401 -> Log.e(TAG, "HTTP 401 Unauthorized: ${request.url}, token geçersiz veya eksik")
                403 -> Log.e(TAG, "HTTP 403 Forbidden: ${request.url}, erişim yetkisi yok")
                404 -> {
                    try {
                        val errorBody = response.peekBody(4096).string()
                        Log.e(TAG, "HTTP 404 Not Found: ${request.url}, endpoint bulunamadı")
                        Log.e(TAG, "404 Hata detayları: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "HTTP 404 yanıt body'si okunamadı: ${e.message}")
                    }
                }
                500 -> {
                    try {
                        val errorBody = response.peekBody(4096).string()
                        Log.e(TAG, "HTTP 500 Error: ${request.url}")
                        Log.e(TAG, "500 Hata detayları: $errorBody")
                        
                        // JWT hatası kontrolü
                        if (errorBody.contains("JWT") || 
                            errorBody.contains("key") || 
                            errorBody.contains("HMAC-SHA") ||
                            errorBody.contains("jsonwebtoken") || 
                            errorBody.contains("WeakKeyException")) {
                            Log.e(TAG, "JWT anahtar hatası tespit edildi! Bu hata sunucu yapılandırmasından kaynaklanıyor.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "HTTP 500 yanıt body'si okunamadı: ${e.message}")
                    }
                }
            }
            
            return@Interceptor response
        } catch (e: IOException) {
            Log.e(TAG, "HTTP hata interceptor: ${e.message}")
            throw e
        }
    }
    
    // Gson instance'ını özelleştiriyoruz
    private val gson = GsonBuilder()
        .setLenient() // JSON formatlama hatalarına karşı daha toleranslı
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(urlLoggingInterceptor) // URL loglaması için ekledik
        .addInterceptor(errorInterceptor)  // Hata interceptor'ını ekledik
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // Zaman aşımı süresini 60 saniyeye çıkardık
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
} 