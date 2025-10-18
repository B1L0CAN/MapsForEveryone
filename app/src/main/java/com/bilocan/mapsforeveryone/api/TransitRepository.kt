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
    private val MAX_RETRIES = 3
    private val RETRY_DELAY_MS = 1000L
    
    /**
     * İki nokta arasındaki toplu taşıma rotasını alır
     * @param origin Başlangıç noktası (lat,lng formatında veya adres)
     * @param destination Varış noktası (lat,lng formatında veya adres)
     * @return İşlem sonucu
     */
    suspend fun getTransitRoute(origin: String, destination: String): Result<TransitResponse> = withContext(Dispatchers.IO) {
        var retryCount = 0
        
        while (retryCount < MAX_RETRIES) {
            try {
                Log.d(TAG, "Toplu taşıma rotası isteniyor: $origin -> $destination (Deneme ${retryCount + 1}/$MAX_RETRIES)")
                
                // Önce parametreleri kontrol et
                if (origin.isBlank() || destination.isBlank()) {
                    Log.e(TAG, "Geçersiz parametreler: origin=$origin, destination=$destination")
                    return@withContext Result.failure(Exception("Başlangıç veya varış noktası geçersiz."))
                }
                
                val response = apiService.getTransitRoute(origin, destination)
                Log.d(TAG, "API yanıt kodu: ${response.code()}")
                Log.d(TAG, "API yanıt başlıkları: ${response.headers()}")
                
                if (response.isSuccessful) {
                    val transitResponse = response.body()
                    Log.d(TAG, "API yanıt body: $transitResponse")
                    
                    if (transitResponse != null) {
                        Log.d(TAG, "Rota başarıyla alındı: ${transitResponse.status}")
                        
                        // API yanıt durumunu kontrol et
                        if (transitResponse.status == "ZERO_RESULTS") {
                            Log.w(TAG, "Rota sonucu bulunamadı (ZERO_RESULTS)")
                            return@withContext Result.failure(Exception("Bu güzergah için toplu taşıma rotası bulunamadı (ZERO_RESULTS)."))
                        }
                        
                        if (transitResponse.error) {
                            Log.e(TAG, "API yanıtında hata var: ${transitResponse.message}")
                            return@withContext Result.failure(Exception(transitResponse.message.ifEmpty { "API yanıtında tanımlanmamış bir hata oluştu." }))
                        }
                        
                        // steps veya route kontrolü
                        if (transitResponse.steps == null || transitResponse.steps?.isEmpty() == true) {
                            Log.d(TAG, "steps null veya boş, route kullanılacak")
                            
                            // Eğer steps null ama route doluysa, steps'e route'u atayalım
                            if (transitResponse.route.isNotEmpty()) {
                                transitResponse.steps = transitResponse.route
                            } else {
                                // Hiçbir rota verisi yoksa boş bir liste oluştur
                                transitResponse.steps = ArrayList()
                                Log.w(TAG, "Rota adım verisi bulunamadı")
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
                        
                        // TransferInfo kontrol
                        if (transitResponse.transferInfo == null) {
                            Log.d(TAG, "transferInfo null, aktarma bilgisi yok")
                        } else {
                            Log.d(TAG, "Aktarma sayısı: ${transitResponse.transferInfo.transferCount}")
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
                        400 -> "Geçersiz istek parametreleri. Adres bilgisi hatalı olabilir."
                        404 -> "Rota veya girdiğiniz adres bulunamadı."
                        405 -> "Bu endpoint desteklenmiyor. Sunucu ayarlarını kontrol edin."
                        500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                        else -> "Rota alınırken bir hata oluştu (HTTP ${response.code()})."
                    }
                    
                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP exception: ${e.code()}, ${e.message()}", e)
                val errorMessage = when(e.code()) {
                    400 -> "Geçersiz istek parametreleri. Adres bilgisi hatalı olabilir."
                    404 -> "Rota veya girdiğiniz adres bulunamadı."
                    500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                    else -> "Rota alınırken bir hata oluştu (HTTP ${e.code()})."
                }
                return@withContext Result.failure(Exception(errorMessage))
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Bağlantı zaman aşımı: ${e.message}", e)
                retryCount++
                
                if (retryCount < MAX_RETRIES) {
                    Log.d(TAG, "Yeniden deneme yapılıyor... (${retryCount + 1}/$MAX_RETRIES)")
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                    continue
                }
                
                return@withContext Result.failure(Exception("Sunucuya bağlanırken zaman aşımı. Lütfen internet bağlantınızı kontrol edin."))
            } catch (e: Exception) {
                Log.e(TAG, "Rota alınırken hata oluştu: ${e.message}", e)
                return@withContext Result.failure(Exception("Rota alınırken bir hata oluştu: ${e.message}. Lütfen tekrar deneyin."))
            }
        }
        
        Result.failure(Exception("Maksimum deneme sayısına ulaşıldı. Lütfen daha sonra tekrar deneyin."))
    }
} 