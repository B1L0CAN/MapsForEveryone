package com.bilocan.mapsforeveryone.api

import android.util.Log
import okhttp3.ResponseBody
import org.json.JSONObject

object ApiResponseLogger {
    private const val TAG = "ApiResponseLogger"
    
    /**
     * API yanıtını detaylı olarak loglar
     */
    fun logResponse(methodName: String, responseBody: ResponseBody?) {
        try {
            val bodyString = responseBody?.string()
            Log.d(TAG, "[$methodName] Yanıt: $bodyString")
            
            // JSON olarak parse etmeyi dene, başarısız olursa sorun değil
            if (!bodyString.isNullOrEmpty()) {
                try {
                    val jsonObject = JSONObject(bodyString)
                    Log.d(TAG, "[$methodName] Yanıt JSON formatında ve geçerli")
                    
                    // Success veya status alanlarını içeriyor mu?
                    val hasSuccess = jsonObject.has("success")
                    val hasStatus = jsonObject.has("status")
                    
                    Log.d(TAG, "[$methodName] Yanıtta 'success' alanı: $hasSuccess")
                    Log.d(TAG, "[$methodName] Yanıtta 'status' alanı: $hasStatus")
                    
                    // Tüm alanları log'a yaz
                    val keys = jsonObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = jsonObject.get(key)
                        Log.d(TAG, "[$methodName] JSON key: $key, value: $value")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[$methodName] Yanıt JSON formatında değil: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$methodName] Yanıt loglama hatası: ${e.message}")
        }
    }
} 