package com.bilocan.mapsforeveryone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bilocan.mapsforeveryone.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), OnInitListener {
    lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var navController: NavController
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    var pendingAddress: String? = null
    var pendingAddressName: String? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun attachBaseContext(newBase: Context) {
        try {
            val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val fontSize = prefs.getString("font_size", "normal")
            val config = newBase.resources.configuration
            val scale = when (fontSize) {
                "large" -> 1.25f
                else -> 1.0f
            }
            config.fontScale = scale
            applyOverrideConfiguration(config)
            super.attachBaseContext(newBase)
        } catch (e: Exception) {
            Log.e(TAG, "attachBaseContext hatası", e)
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // RetrofitClient'ı başlat
            com.bilocan.mapsforeveryone.api.RetrofitClient.init(applicationContext)
            com.bilocan.mapsforeveryone.api.RetrofitClient.clearClientState()

            // TTS'i öncelikle başlat (sesli bildirimler için)
            setupTTS()
            
            // Giriş durumunu kontrol et
            prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val rememberMe = prefs.getBoolean("remember_me", false)

            // NavController ve bottom navigation setup
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            binding.bottomNavigationView.setupWithNavController(navController)

            // Giriş durumuna göre UI ayarla
            if (!isLoggedIn || !rememberMe) {
                // Giriş yapılmamış veya beni hatırla seçili değil, alt navigasyon barını gizle
                binding.bottomNavigationView.visibility = View.GONE
                
                // TTS başlatıldığında otomatik olarak ilk ekran bildirimini yapmak için
                // Küçük bir gecikme ile ilk sesli bildirimi yap
                binding.root.postDelayed({
                    speak("Giriş yapma ekranı")
                }, 1500) // 1.5 saniye gecikme - TTS hazır olsun diye
            } else {
                // Giriş yapılmış ve beni hatırla seçili, alt navigasyon barını göster
                binding.bottomNavigationView.visibility = View.VISIBLE
            }

            setupViews()
            applyContrastMode(prefs.getBoolean("contrast", false))
        } catch (e: Exception) {
            Log.e(TAG, "onCreate hatası", e)
            Toast.makeText(this, "Uygulama başlatılırken bir hata oluştu", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupTTS() {
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS başlatma hatası", e)
            Toast.makeText(this, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViews() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_NONE)
                    transaction.replace(R.id.nav_host_fragment, com.bilocan.mapsforeveryone.ui.home.HomeFragment())
                    transaction.commitNow()
                    speak("Ana Sayfa")
                    true
                }
                R.id.navigation_favorites -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_NONE)
                    transaction.replace(R.id.nav_host_fragment, com.bilocan.mapsforeveryone.ui.favorites.FavoritesFragment())
                    transaction.commitNow()
                    speak("Favoriler")
                    true
                }
                R.id.navigation_settings -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_NONE)
                    transaction.replace(R.id.nav_host_fragment, com.bilocan.mapsforeveryone.ui.settings.SettingsFragment())
                    transaction.commitNow()
                    speak("Ayarlar")
                    true
                }
                else -> false
            }
        }
    }

    fun startVoiceRecognition() {
        try {
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Konuşun...")
            
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            Toast.makeText(this, "Ses tanıma başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (results != null && results.isNotEmpty()) {
                val recognizedText = results[0]
                processVoiceCommand(recognizedText)
            }
        }
    }

    private fun processVoiceCommand(command: String) {
        if (isTtsInitialized && tts != null) {
            speak("Tanınan komut: $command")
        }
    }

    fun speak(text: String) {
        try {
            if (isTtsInitialized && tts != null) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Konuşma hatası", e)
        }
    }

    override fun onInit(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                val turkishLocale = Locale("tr", "TR")
                val result = tts?.setLanguage(turkishLocale)
                
                when (result) {
                    TextToSpeech.LANG_MISSING_DATA -> {
                        val installIntent = Intent()
                        installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                        startActivity(installIntent)
                        Toast.makeText(this, "Türkçe dil paketi yükleniyor...", Toast.LENGTH_LONG).show()
                    }
                    TextToSpeech.LANG_NOT_SUPPORTED -> {
                        tts?.setLanguage(Locale.getDefault())
                        Toast.makeText(this, "Türkçe desteklenmiyor, varsayılan dil kullanılıyor", Toast.LENGTH_SHORT).show()
                    }
                    TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                        isTtsInitialized = true
                    }
                }
            } else {
                Toast.makeText(this, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS başlatma hatası", e)
            Toast.makeText(this, "Ses sentezi başlatılırken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyContrastMode(enabled: Boolean) {
        try {
            if (enabled) {
                window.decorView.setBackgroundColor(0xFF000000.toInt())
            } else {
                window.decorView.setBackgroundColor(0xFF121212.toInt())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kontrast modu uygulama hatası", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // RetrofitClient'ı yeniden başlat
            com.bilocan.mapsforeveryone.api.RetrofitClient.clearClientState()
            
            // Giriş durumunu kontrol et ve gerekirse navigasyon yap
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val rememberMe = prefs.getBoolean("remember_me", false)
            
            // Bottom navigation barını güncelle
            binding.bottomNavigationView.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            
            // Giriş durumuna göre uygun sayfaya yönlendir - onResume'da daha güvenli
            if (isLoggedIn && rememberMe) {
                // Şu an login ekranındaysa ve giriş yapmışsa ana sayfaya yönlendir
                if (navController.currentDestination?.id == R.id.navigation_login) {
                    // Ana sayfaya git
                    navController.navigate(R.id.action_login_to_home)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "RetrofitClient'ı yeniden başlatma hatası", e)
        }
    }

    // Kullanıcı çıkış yapma fonksiyonu
    fun logout() {
        try {
            // Çıkış yapıldı bilgisi kaydet
            prefs.edit()
                .putBoolean("is_logged_in", false)
                .putBoolean("remember_me", false)
                .apply()
            
            // Bottom navigation'ı gizle
            binding.bottomNavigationView.visibility = View.GONE
            
            // Kullanıcıya bilgi ver
            Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
            
            // Login sayfasına zorla yönlendir
            // Önce mevcut fragmentları temizle
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            
            // Sonra login fragment'ını ekle
            val loginFragment = com.bilocan.mapsforeveryone.ui.login.LoginFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, loginFragment)
                .commitNow()
            
            // Ek olarak uygulama durumunu sıfırla
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Çıkış yapma hatası: ${e.message}", e)
            Toast.makeText(this, "Çıkış yapılırken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "TTS kapatma hatası", e)
        }
        super.onDestroy()
    }

    fun showAddressOnHome(address: String) {
        pendingAddress = address
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val homeFragment = navHostFragment?.childFragmentManager?.fragments?.find { it is com.bilocan.mapsforeveryone.ui.home.HomeFragment } as? com.bilocan.mapsforeveryone.ui.home.HomeFragment
        homeFragment?.let {
            it.navigateToAddress(address)
            pendingAddress = null
        }
    }

    fun onUserLoggedIn() {
        try {
            // Giriş durumunu kaydet
            prefs.edit().putBoolean("is_logged_in", true).apply()
            
            // Bottom navigation'ı göster
            binding.bottomNavigationView.visibility = View.VISIBLE
            
            // Ana sayfaya hızlı geçiş - NavHostFragment'ı alıp direkt HomeFragment'a değiştiriyoruz
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.childFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.bilocan.mapsforeveryone.ui.home.HomeFragment())
                .commitNow()
                
            // Kullanıcıya başarılı mesajı göster
            Toast.makeText(this, "Başarıyla giriş yaptınız!", Toast.LENGTH_SHORT).show()
            
            // Ana sayfa seçildiğini işaretle
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            setupViews()
            
            // Ana sayfa açıldı bilgisini kullanıcıya söyle
            speak("Ana Sayfa")
        } catch (e: Exception) {
            Log.e(TAG, "Giriş yapma hatası: ${e.message}", e)
            Toast.makeText(this, "Giriş yapılırken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
}