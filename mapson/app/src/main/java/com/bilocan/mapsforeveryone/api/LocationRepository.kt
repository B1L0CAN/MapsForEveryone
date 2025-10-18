package com.bilocan.mapsforeveryone.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bilocan.mapsforeveryone.api.model.LocationListResponse
import com.bilocan.mapsforeveryone.api.model.LocationRequest
import com.bilocan.mapsforeveryone.api.model.LocationResponse
import com.bilocan.mapsforeveryone.data.FavoriteLocation
import com.bilocan.mapsforeveryone.data.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException

class LocationRepository(private val context: Context) : FavoriteRepository {
    private val TAG = "LocationRepository"
    private val apiService = RetrofitClient.apiService
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Cache için favori listesi
    private var cachedFavorites: List<FavoriteLocation> = emptyList()
    
    private fun getToken(): String {
        return prefs.getString("user_token", "") ?: ""
    }
    
    private fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }
    
    // Cache güncelleme
    private fun updateCachedFavorites(favorites: List<FavoriteLocation>) {
        cachedFavorites = favorites
    }
    
    // API'den favorileri çekme
    suspend fun fetchFavoritesFromApi(): Result<List<FavoriteLocation>> = withContext(Dispatchers.IO) {
        try {
            // Kullanıcı giriş yapmamışsa boş liste dön
            if (!isLoggedIn()) {
                Log.d(TAG, "Kullanıcı giriş yapmamış, favoriler çekilemiyor")
                return@withContext Result.success(emptyList())
            }
            
            val token = getToken()
            
            if (token.isEmpty()) {
                Log.d(TAG, "Token bulunamadı, favoriler çekilemiyor")
                return@withContext Result.failure(Exception("Oturum bilgileriniz bulunamadı. Lütfen tekrar giriş yapın."))
            }
            
            Log.d(TAG, "API'den favoriler çekiliyor, token: $token")
            
            try {
                val response = apiService.getLocations()
                
                if (response.isSuccessful) {
                    val locationsList = response.body()
                    
                    if (locationsList != null) {
                        // API doğrudan LocationResponse listesi dönüyor
                        val favorites = locationsList.map { locationResponse ->
                            FavoriteLocation(
                                id = locationResponse.id,
                                name = locationResponse.name,
                                address = locationResponse.address,
                                latitude = locationResponse.latitude,
                                longitude = locationResponse.longitude,
                                notes = locationResponse.notes ?: ""
                            )
                        }
                        
                        // Cache'i güncelle
                        updateCachedFavorites(favorites)
                        
                        Log.d(TAG, "API'den ${favorites.size} favori çekildi")
                        return@withContext Result.success(favorites)
                    } else {
                        Log.e(TAG, "API yanıt body null")
                        return@withContext Result.failure(Exception("Sunucudan yanıt alınamadı. Lütfen tekrar deneyin."))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Bilinmeyen hata"
                    Log.e(TAG, "API hatası: $errorBody, HTTP kodu: ${response.code()}")
                    
                    val errorMessage = when(response.code()) {
                        401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                        403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                        404 -> "Favori adresleri bulunamadı. Sunucu yanıt vermiyor."
                        500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                        else -> "Favoriler çekilirken bir hata oluştu: ${response.code()}"
                    }
                    
                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "API isteği sırasında beklenmeyen hata: ${e.message}", e)
                return@withContext Result.failure(Exception("Sunucuyla iletişim kurulurken hata oluştu: ${e.message}"))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception: ${e.code()}, ${e.message()}", e)
            val errorMessage = when(e.code()) {
                401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                404 -> "Favori adresleri bulunamadı. Sunucu yanıt vermiyor."
                500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                else -> "Favoriler çekilirken bir hata oluştu: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Bağlantı zaman aşımı: ${e.message}", e)
            Result.failure(Exception("Sunucuya bağlanırken zaman aşımı. Lütfen internet bağlantınızı kontrol edin."))
        } catch (e: Exception) {
            Log.e(TAG, "Favoriler çekilirken hata oluştu: ${e.message}", e)
            Result.failure(Exception("Favoriler yüklenirken bir hata oluştu. Lütfen tekrar deneyin."))
        }
    }
    
    // Yeni favori oluşturma
    suspend fun createFavoriteOnApi(favorite: FavoriteLocation): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Kullanıcı giriş yapmamışsa hata dön
            if (!isLoggedIn()) {
                Log.e(TAG, "Kullanıcı giriş yapmamış, favori kaydedilemiyor")
                return@withContext Result.failure(Exception("Favori kaydetmek için giriş yapmalısınız"))
            }
            
            val token = getToken()
            
            if (token.isEmpty()) {
                Log.d(TAG, "Token bulunamadı, favori kaydedilemiyor")
                return@withContext Result.failure(Exception("Oturum bilgileriniz bulunamadı. Lütfen tekrar giriş yapın."))
            }
            
            // LocationRequest nesnesini oluştur
            val locationRequest = LocationRequest(
                name = favorite.name,
                address = favorite.address,
                latitude = favorite.latitude,
                longitude = favorite.longitude,
                notes = favorite.notes
            )
            
            // Daha detaylı loglama
            Log.d(TAG, "Favori kaydediliyor...")
            Log.d(TAG, "Kayıt detayları: $favorite")
            Log.d(TAG, "API'ye gönderilecek veriler: $locationRequest")
            Log.d(TAG, "Timestamp değeri: ${locationRequest.timestamp}")
            Log.d(TAG, "Bearer Token kullanılıyor: ${token.take(15)}...")
            
            val response = apiService.createLocation(locationRequest)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d(TAG, "Favori başarıyla kaydedildi: ${responseBody}")
                
                // Cache'i güncelle - API'den tüm favori listesini tekrar çekerek
                val result = fetchFavoritesFromApi()
                Log.d(TAG, "Favoriler güncellendi, yeni liste uzunluğu: ${getAllFavorites().size}")
                
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API hatası: $errorBody, HTTP kodu: ${response.code()}")
                
                val errorMessage = when(response.code()) {
                    401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                    403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                    404 -> "Favori kayıt adresi bulunamadı. Sunucu yanıt vermiyor."
                    500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                    else -> "Favori kaydedilirken bir hata oluştu: ${response.code()}"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception: ${e.code()}, ${e.message()}", e)
            val errorMessage = when(e.code()) {
                401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                404 -> "Favori kayıt adresi bulunamadı. Sunucu yanıt vermiyor."
                500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                else -> "Favori kaydedilirken bir hata oluştu: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Bağlantı zaman aşımı: ${e.message}", e)
            Result.failure(Exception("Sunucuya bağlanırken zaman aşımı. Lütfen internet bağlantınızı kontrol edin."))
        } catch (e: Exception) {
            Log.e(TAG, "Favori kaydedilirken hata oluştu: ${e.message}", e)
            Result.failure(Exception("Favori kaydedilirken bir hata oluştu. Lütfen tekrar deneyin."))
        }
    }
    
    // Favori güncelleme
    suspend fun updateFavoriteOnApi(favorite: FavoriteLocation): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Kullanıcı giriş yapmamışsa hata dön
            if (!isLoggedIn()) {
                Log.e(TAG, "Kullanıcı giriş yapmamış, favori güncellenemiyor")
                return@withContext Result.failure(Exception("Favori güncellemek için giriş yapmalısınız"))
            }
            
            val token = getToken()
            
            if (token.isEmpty()) {
                Log.e(TAG, "Token bulunamadı, favori güncellenemiyor")
                return@withContext Result.failure(Exception("Oturum bilgileriniz bulunamadı. Lütfen tekrar giriş yapın."))
            }
            
            // Favori ID'si yoksa hata dön
            if (favorite.id.isEmpty()) {
                Log.e(TAG, "Favori ID bulunamadı, güncellenemiyor")
                return@withContext Result.failure(Exception("Favori ID bulunamadı"))
            }
            
            // LocationRequest nesnesini oluştur
            val locationRequest = LocationRequest(
                name = favorite.name,
                address = favorite.address,
                latitude = favorite.latitude,
                longitude = favorite.longitude,
                notes = favorite.notes
            )
            
            Log.d(TAG, "API'de favori güncelleniyor: $locationRequest, ID: ${favorite.id}")
            Log.d(TAG, "Timestamp değeri: ${locationRequest.timestamp}")
            
            val response = apiService.updateLocation(favorite.id, locationRequest)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Favori başarıyla güncellendi: ${response.body()}")
                
                // Cache'i güncelle - API'den tüm favori listesini tekrar çekerek
                fetchFavoritesFromApi()
                
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API hatası: $errorBody, HTTP kodu: ${response.code()}")
                
                val errorMessage = when(response.code()) {
                    401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                    403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                    404 -> "Favori kaydı bulunamadı."
                    500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                    else -> "Favori güncellenirken bir hata oluştu: ${response.code()}"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception: ${e.code()}, ${e.message()}", e)
            val errorMessage = when(e.code()) {
                401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                404 -> "Favori kaydı bulunamadı."
                500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                else -> "Favori güncellenirken bir hata oluştu: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Bağlantı zaman aşımı: ${e.message}", e)
            Result.failure(Exception("Sunucuya bağlanırken zaman aşımı. Lütfen internet bağlantınızı kontrol edin."))
        } catch (e: Exception) {
            Log.e(TAG, "Favori güncellenirken hata oluştu: ${e.message}", e)
            Result.failure(Exception("Favori güncellenirken bir hata oluştu. Lütfen tekrar deneyin."))
        }
    }
    
    // Favori silme
    suspend fun deleteFavoriteFromApi(favorite: FavoriteLocation): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Kullanıcı giriş yapmamışsa hata dön
            if (!isLoggedIn()) {
                Log.e(TAG, "Kullanıcı giriş yapmamış, favori silinemiyor")
                return@withContext Result.failure(Exception("Favori silmek için giriş yapmalısınız"))
            }
            
            val token = getToken()
            
            if (token.isEmpty()) {
                Log.e(TAG, "Token bulunamadı, favori silinemiyor")
                return@withContext Result.failure(Exception("Oturum bilgileriniz bulunamadı. Lütfen tekrar giriş yapın."))
            }
            
            // Favori adı yoksa hata dön
            if (favorite.name.isEmpty()) {
                Log.e(TAG, "Favori adı bulunamadı, silinemiyor")
                return@withContext Result.failure(Exception("Favori adı bulunamadı"))
            }
            
            Log.d(TAG, "API'den favori siliniyor, Name: ${favorite.name}")
            val response = apiService.deleteLocation(favorite.name)
            
            if (response.isSuccessful) {
                // Cache'i güncelle
                val favorites = getAllFavorites().toMutableList()
                favorites.removeIf { it.id == favorite.id }
                updateCachedFavorites(favorites)
                
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API hatası: $errorBody, HTTP kodu: ${response.code()}")
                
                val errorMessage = when(response.code()) {
                    401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                    403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                    404 -> "Favori kaydı bulunamadı."
                    500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                    else -> "Favori silinirken bir hata oluştu: ${response.code()}"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception: ${e.code()}, ${e.message()}", e)
            val errorMessage = when(e.code()) {
                401 -> "Oturum süreniz dolmuş. Lütfen tekrar giriş yapın."
                403 -> "Bu işlem için yetkiniz bulunmuyor. Lütfen tekrar giriş yapın."
                404 -> "Favori kaydı bulunamadı."
                500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                else -> "Favori silinirken bir hata oluştu: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Bağlantı zaman aşımı: ${e.message}", e)
            Result.failure(Exception("Sunucuya bağlanırken zaman aşımı. Lütfen internet bağlantınızı kontrol edin."))
        } catch (e: Exception) {
            Log.e(TAG, "Favori silinirken hata oluştu: ${e.message}", e)
            Result.failure(Exception("Favori silinirken bir hata oluştu. Lütfen tekrar deneyin."))
        }
    }
    
    // FavoriteRepository arayüzünü uygulamak için gerekli metotlar
    override fun getAllFavorites(): List<FavoriteLocation> {
        return cachedFavorites
    }
    
    override fun addFavorite(favorite: FavoriteLocation) {
        // API'ye ekleme ve cache güncelleme işlemleri farklı bir coroutine içinde yapılacak
        // Bu metot sadece mevcut cache'i döndürüyor
    }
    
    override fun updateFavorite(favorite: FavoriteLocation) {
        // API'de güncelleme ve cache güncelleme işlemleri farklı bir coroutine içinde yapılacak
        // Bu metot sadece mevcut cache'i döndürüyor
    }
    
    override fun deleteFavorite(favorite: FavoriteLocation) {
        // API'den silme ve cache güncelleme işlemleri farklı bir coroutine içinde yapılacak
        // Bu metot sadece mevcut cache'i döndürüyor
    }
    
    override fun getFavoriteById(id: String): FavoriteLocation? {
        return cachedFavorites.find { it.id == id }
    }
} 