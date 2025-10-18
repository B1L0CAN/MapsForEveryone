package com.bilocan.mapsforeveryone.ui.favorites

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.api.LocationRepository
import com.bilocan.mapsforeveryone.data.FavoriteLocation
import com.bilocan.mapsforeveryone.data.SharedPreferencesFavoriteRepository
import com.bilocan.mapsforeveryone.databinding.FragmentFavoritesBinding
import java.util.Locale

class FavoritesFragment : Fragment(), OnInitListener, AddFavoriteDialog.OnFavoriteAddedListener {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels {
        // Retrofit kullanarak API'ye bağlanacak bir repository oluştur
        FavoritesViewModelFactory(LocationRepository(requireContext()))
    }
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var adapter: FavoritesAdapter? = null
    private var lastClickTime = 0L
    private var lastClickedView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            setupTTS()
            setupRecyclerView()
            observeViewModel()
            
            // Favori ekle butonu için çift tıklama kontrolü
            binding.addFavoriteButton.setOnClickListener { view ->
                handleDoubleClick(view, "Favori ekle butonu. Çift tıklayarak yeni favori noktası ekleyebilirsiniz.") {
                    if (isTtsInitialized && tts != null) {
                        try {
                            showAddFavoriteDialog()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Favori ekleme penceresi açılamadı: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Ses sentezi hazır değil", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Kontrast modunu uygula
            updateAccessibilitySettings()
        } catch (e: Exception) {
            Toast.makeText(context, "Favoriler ekranı başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleDoubleClick(view: View, description: String, action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (lastClickedView == view && currentTime - lastClickTime < 500) {
            // Çift tıklama algılandı, işlemi gerçekleştir
            action()
            lastClickTime = 0
            lastClickedView = null
        } else {
            // İlk tıklama, butonun ne olduğunu söyle
            speak(description)
            lastClickTime = currentTime
            lastClickedView = view
        }
    }

    private fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (e: Exception) {
                Toast.makeText(context, "Konuşma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter(
            onEditClick = { favorite ->
                handleDoubleClick(binding.root, "${favorite.name} favori noktasını düzenleme butonu. Çift tıklayarak düzenleyebilirsiniz.") {
                    showEditDialog(favorite)
                }
            },
            onDeleteClick = { favorite ->
                handleDoubleClick(binding.root, "${favorite.name} favori noktasını silme butonu. Çift tıklayarak silebilirsiniz.") {
                    showDeleteDialog(favorite)
                }
            },
            onNavigateClick = { favorite ->
                handleDoubleClick(binding.root, "${favorite.name} adresine gitmek için iki defa basınız.") {
                    startNavigation(favorite)
                }
            },
            tts = tts
        )
        
        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FavoritesFragment.adapter
        }
    }

    private fun setupTTS() {
        try {
            tts = TextToSpeech(requireContext().applicationContext, this)
        } catch (e: Exception) {
            Toast.makeText(context, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Favoriler için observer
        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            adapter?.submitList(favorites)
            
            // Boş durum kontrolü
            binding.noFavoritesText.isVisible = favorites.isEmpty()
            binding.favoritesRecyclerView.isVisible = favorites.isNotEmpty()
            
            if (isTtsInitialized && tts != null) {
                if (favorites.isEmpty()) {
                    speak("Favori nokta bulunamadı")
                } else {
                    speak("${favorites.size} adet favori nokta listeleniyor")
                }
            }
        }
        
        // Yükleniyor durumu için observer
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.addFavoriteButton.isEnabled = !isLoading
        }
        
        // Hata durumu için observer
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startNavigation(favorite: FavoriteLocation) {
        try {
            // MainActivity'ye favoriye gitme isteğini ilet
            (activity as? com.bilocan.mapsforeveryone.MainActivity)?.let { mainActivity ->
                // Önce adresi ve başlığı sakla
                mainActivity.pendingAddress = favorite.address
                mainActivity.pendingAddressName = favorite.name  // Başlık bilgisini de gönder
                
                // Sesli bildirimi yap
                speak("Favorilerinizde bulunan ${favorite.name} adresine rota başlatılıyor.")
                
                // Ana sayfaya manuel olarak geç (doğrudan fragment değiştirerek)
                val homeFragment = com.bilocan.mapsforeveryone.ui.home.HomeFragment()
                
                // Bundle'a adres ve başlık bilgilerini ekle
                val bundle = Bundle()
                bundle.putString("navigate_to_address", favorite.address)
                bundle.putString("navigate_name", favorite.name)  // Başlık bilgisini de gönder
                homeFragment.arguments = bundle
                
                // Fragment'ı değiştir
                val fragmentManager = mainActivity.supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_NONE)
                transaction.replace(R.id.nav_host_fragment, homeFragment)
                transaction.commitNow()
                
                // BottomNavigationView'i güncelle - ana sayfa seçili olarak
                mainActivity.binding.bottomNavigationView.menu.findItem(R.id.navigation_home).isChecked = true
            } ?: run {
                // MainActivity bulunamadı
                speak("Navigasyon başlatılamadı.")
                Toast.makeText(context, "Navigasyon başlatılamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            speak("Navigasyon başlatılırken bir hata oluştu.")
            Toast.makeText(context, "Navigasyon başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(favorite: FavoriteLocation) {
        if (isTtsInitialized && tts != null) {
            EditFavoriteDialog(
                favorite = favorite,
                onSave = { updatedFavorite ->
                    viewModel.updateFavorite(updatedFavorite)
                    speak("${updatedFavorite.name} favori noktası güncellendi")
                },
                tts = tts
            ).show(childFragmentManager, "edit_dialog")
        }
    }

    private fun showDeleteDialog(favorite: FavoriteLocation) {
        if (isTtsInitialized && tts != null) {
            DeleteFavoriteDialog(
                favorite = favorite,
                onDelete = {
                    viewModel.deleteFavorite(favorite)
                    speak("${favorite.name} favori noktası silindi")
                },
                tts = tts
            ).show(childFragmentManager, "delete_dialog")
        }
    }

    private fun showAddFavoriteDialog() {
        val dialog = AddFavoriteDialog()
        dialog.show(childFragmentManager, "AddFavoriteDialog")
    }

    override fun onFavoriteAdded(name: String, address: String) {
        val favorite = FavoriteLocation(
            name = name,
            address = address,
            latitude = 0.0,
            longitude = 0.0
        )
        viewModel.addFavorite(favorite)
        speak("${name} favori noktası eklendi")
    }

    override fun onResume() {
        super.onResume()
        if (isTtsInitialized) {
            viewModel.loadFavorites()
        }
        // Kontrast modunu güncelle
        updateAccessibilitySettings()
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
        tts = null
        _binding = null
    }

    fun updateAccessibilitySettings() {
        try {
            val prefs = requireContext().getSharedPreferences("settings", 0)
            val contrast = prefs.getBoolean("contrast", false)
            val fontSize = prefs.getString("font_size", "normal")
            val context = requireContext()
            
            val bgColor = if (contrast) context.getColor(R.color.high_contrast_black) else context.getColor(R.color.dark_background)
            val cardColor = if (contrast) context.getColor(R.color.high_contrast_black) else context.getColor(R.color.dark_card)
            val textColor = if (contrast) context.getColor(R.color.high_contrast_yellow) else context.getColor(R.color.high_contrast_white)
            val buttonColor = if (contrast) context.getColor(R.color.high_contrast_yellow) else context.getColor(R.color.dark_button)
            val buttonTextColor = if (contrast) context.getColor(R.color.high_contrast_black) else context.getColor(R.color.high_contrast_black)

            binding.root.setBackgroundColor(bgColor)
            binding.favoritesCard.setCardBackgroundColor(cardColor)
            binding.favoritesTitle.setTextColor(textColor)
            binding.addFavoriteButton.setBackgroundColor(buttonColor)
            binding.addFavoriteButton.setTextColor(buttonTextColor)

            val scale = if (fontSize == "large") 1.35f else 1.0f
            binding.favoritesTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f * scale)
            binding.addFavoriteButton.textSize = 20f * scale

            binding.root.invalidate()
            requireActivity().window.decorView.invalidate()
        } catch (e: Exception) {
            Toast.makeText(context, "Erişilebilirlik ayarları güncellenemedi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val turkishLocale = Locale("tr", "TR")
                val result = tts?.setLanguage(turkishLocale)
                
                when (result) {
                    TextToSpeech.LANG_MISSING_DATA -> {
                        // Türkçe dil paketi eksik, indirme isteği gönder
                        val installIntent = Intent()
                        installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                        startActivity(installIntent)
                        Toast.makeText(context, "Türkçe dil paketi yükleniyor...", Toast.LENGTH_LONG).show()
                    }
                    TextToSpeech.LANG_NOT_SUPPORTED -> {
                        // Türkçe desteklenmiyor, varsayılan dili kullan
                        tts?.setLanguage(Locale.getDefault())
                        Toast.makeText(context, "Türkçe desteklenmiyor, varsayılan dil kullanılıyor", Toast.LENGTH_SHORT).show()
                    }
                    TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                        // Türkçe başarıyla ayarlandı
                        isTtsInitialized = true
                        viewModel.loadFavorites()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Dil ayarları yapılandırılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }
} 