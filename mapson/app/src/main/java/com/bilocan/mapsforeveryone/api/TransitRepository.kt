package com.bilocan.mapsforeveryone.api

import android.content.Context
import android.util.Log
import com.bilocan.mapsforeveryone.api.model.RouteStep
import com.bilocan.mapsforeveryone.api.model.TransitResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.util.ArrayList

class TransitRepository(private val context: Context) {
    private val TAG = "TransitRepository"
    private val apiService = RetrofitClient.apiService
    
    /**
     * İki nokta arasındaki toplu taşıma rotasını alır
     * @param origin Başlangıç noktası (lat,lng formatında veya adres)
     * @param destination Varış noktası (lat,lng formatında veya adres)
     * @return İşlem sonucu
     */
    suspend fun getTransitRoute(origin: String, destination: String): Result<TransitResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Toplu taşıma rotası isteniyor: $origin -> $destination")
            
            val response = apiService.getTransitRoute(origin, destination)
            
            if (response.isSuccessful) {
                val transitResponse = response.body()
                
                if (transitResponse != null) {
                    Log.d(TAG, "Rota başarıyla alındı: ${transitResponse.status}")
                    
                    // steps veya route kontrolü
                    if (transitResponse.steps == null || transitResponse.steps?.isEmpty() == true) {
                        Log.d(TAG, "steps null veya boş, route kullanılacak")
                        
                        // Eğer steps null ama route doluysa, steps'e route'u atayalım
                        if (transitResponse.route.isNotEmpty()) {
                            transitResponse.steps = transitResponse.route
                        } else {
                            // Hiçbir rota verisi yoksa boş bir liste oluştur
                            transitResponse.steps = ArrayList()
                        }
                    }
                    
                    // summary ve total değerleri kontrolü
                    if (transitResponse.summary != null) {
                        if (transitResponse.totalDistance.isEmpty() && !transitResponse.summary.totalDistance.isEmpty()) {
                            val newResponse = transitResponse.copy(
                                totalDistance = transitResponse.summary.totalDistance,
                                totalDuration = transitResponse.summary.totalDuration
                            )
                            return@withContext Result.success(newResponse)
                        }
                    }
                    
                    Log.d(TAG, "Adım sayısı: ${transitResponse.steps?.size ?: 0}")
                    return@withContext Result.success(transitResponse)
                } else {
                    Log.e(TAG, "API yanıt body null")
                    return@withContext Result.failure(Exception("Sunucudan yanıt alınamadı. Lütfen tekrar deneyin."))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Bilinmeyen hata"
                Log.e(TAG, "API hatası: $errorBody, HTTP kodu: ${response.code()}")
                
                val errorMessage = when(response.code()) {
                    400 -> "Geçersiz istek parametreleri."
                    404 -> "Rota bulunamadı."
                    405 -> "Bu endpoint desteklenmiyor. Sunucu ayarlarını kontrol edin."
                    500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                    else -> "Rota alınırken bir hata oluştu: ${response.code()}"
                }
                
                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception: ${e.code()}, ${e.message()}", e)
            val errorMessage = when(e.code()) {
                400 -> "Geçersiz istek parametreleri."
                404 -> "Rota bulunamadı."
                500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                else -> "Rota alınırken bir hata oluştu: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Bağlantı zaman aşımı: ${e.message}", e)
            Result.failure(Exception("Sunucuya bağlanırken zaman aşımı. Lütfen internet bağlantınızı kontrol edin."))
        } catch (e: Exception) {
            Log.e(TAG, "Rota alınırken hata oluştu: ${e.message}", e)
            Result.failure(Exception("Rota alınırken bir hata oluştu. Lütfen tekrar deneyin."))
        }
    }
} 