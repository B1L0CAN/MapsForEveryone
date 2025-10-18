package com.bilocan.mapsforeveryone.ui.favorites

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilocan.mapsforeveryone.api.LocationRepository
import com.bilocan.mapsforeveryone.data.FavoriteLocation
import com.bilocan.mapsforeveryone.data.FavoriteRepository
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: FavoriteRepository
) : ViewModel() {

    private val _favorites = MutableLiveData<List<FavoriteLocation>>()
    val favorites: LiveData<List<FavoriteLocation>> = _favorites
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val TAG = "FavoritesViewModel"

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        _isLoading.value = true
        _error.value = null
        
        // Önce cache'den yükle
        _favorites.value = repository.getAllFavorites()
        
        // Eğer repository LocationRepository tipindeyse, API'den de veri çek
        if (repository is LocationRepository) {
            viewModelScope.launch {
                try {
                    val result = repository.fetchFavoritesFromApi()
                    result.fold(
                        onSuccess = { serverFavorites ->
                            _favorites.value = serverFavorites
                            _isLoading.value = false
                        },
                        onFailure = { exception ->
                            _error.value = "Favoriler yüklenirken bir hata oluştu: ${exception.message}"
                            _isLoading.value = false
                        }
                    )
                } catch (e: Exception) {
                    _error.value = "Favoriler yüklenirken bir hata oluştu: ${e.message}"
                    _isLoading.value = false
                }
            }
        } else {
            // LocationRepository değilse sadece local'den yükle
            _isLoading.value = false
        }
    }

    fun addFavorite(favorite: FavoriteLocation) {
        _isLoading.value = true
        _error.value = null
        
        // Önce local'e kaydet
        repository.addFavorite(favorite)
        _favorites.value = repository.getAllFavorites()
        
        // Eğer repository LocationRepository tipindeyse, API'ye de gönder
        if (repository is LocationRepository) {
            viewModelScope.launch {
                try {
                    val result = repository.createFavoriteOnApi(favorite)
                    result.fold(
                        onSuccess = { serverFavorite ->
                            loadFavorites() // Güncel listeyi API'den çek
                        },
                        onFailure = { exception ->
                            _error.value = "Favori sunucuya kaydedilemedi: ${exception.message}"
                            _isLoading.value = false
                        }
                    )
                } catch (e: Exception) {
                    _error.value = "Favori eklenirken bir hata oluştu: ${e.message}"
                    _isLoading.value = false
                }
            }
        } else {
            _isLoading.value = false
        }
    }

    fun updateFavorite(favorite: FavoriteLocation) {
        _isLoading.value = true
        _error.value = null
        
        // Önce local'de güncelle
        repository.updateFavorite(favorite)
        _favorites.value = repository.getAllFavorites()
        
        // Eğer repository LocationRepository tipindeyse, API'de de güncelle
        if (repository is LocationRepository) {
            viewModelScope.launch {
                try {
                    val result = repository.updateFavoriteOnApi(favorite)
                    result.fold(
                        onSuccess = { serverFavorite ->
                            loadFavorites() // Güncel listeyi API'den çek
                        },
                        onFailure = { exception ->
                            _error.value = "Favori sunucuda güncellenemedi: ${exception.message}"
                            _isLoading.value = false
                        }
                    )
                } catch (e: Exception) {
                    _error.value = "Favori güncellenirken bir hata oluştu: ${e.message}"
                    _isLoading.value = false
                }
            }
        } else {
            _isLoading.value = false
        }
    }

    fun deleteFavorite(favorite: FavoriteLocation) {
        _isLoading.value = true
        _error.value = null
        
        // Önce local'den sil
        repository.deleteFavorite(favorite)
        _favorites.value = repository.getAllFavorites()
        
        // Eğer repository LocationRepository tipindeyse, API'den de sil
        if (repository is LocationRepository) {
            viewModelScope.launch {
                try {
                    val result = repository.deleteFavoriteFromApi(favorite)
                    result.fold(
                        onSuccess = { success ->
                            loadFavorites() // Güncel listeyi API'den çek
                        },
                        onFailure = { exception ->
                            _error.value = "Favori sunucudan silinemedi: ${exception.message}"
                            _isLoading.value = false
                        }
                    )
                } catch (e: Exception) {
                    _error.value = "Favori silinirken bir hata oluştu: ${e.message}"
                    _isLoading.value = false
                }
            }
        } else {
            _isLoading.value = false
        }
    }
} 