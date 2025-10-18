package com.bilocan.mapsforeveryone.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.databinding.FragmentSettingsBinding
import java.util.Locale

class SettingsFragment : Fragment(), OnInitListener {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var lastClickTime = 0L
    private var lastClickedView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTTS()
        prefs = requireContext().getSharedPreferences("settings", 0)
        updateAccessibilitySettings()

        // Geri bildirim butonu
        binding.feedbackButton.setOnClickListener { view ->
            handleDoubleClick(view, "Geri bildirim gönderme butonu. Çift tıklayarak e-posta gönderebilirsiniz.") {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("musabilalyaz@hotmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Görme Engelli Uygulaması Geri Bildirim")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "E-posta uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Titreşim ayarı
        binding.vibrationSwitch.isChecked = prefs.getBoolean("vibration", true)
        binding.vibrationSwitch.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                handleDoubleClick(v, "Titreşim ayarı. Çift tıklayarak açıp kapatabilirsiniz.") {
                    binding.vibrationSwitch.isChecked = !binding.vibrationSwitch.isChecked
                    prefs.edit { putBoolean("vibration", binding.vibrationSwitch.isChecked) }
                    updateAccessibilitySettings()
                }
            }
            true
        }

        // Yazı boyutu ayarı
        val fontSize = prefs.getString("font_size", "normal")
        when (fontSize) {
            "normal" -> binding.fontSizeGroup.check(binding.fontNormal.id)
            "large" -> binding.fontSizeGroup.check(binding.fontLarge.id)
        }

        binding.fontNormal.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                handleDoubleClick(v, "Normal yazı boyutu. Çift tıklayarak seçebilirsiniz.") {
                    binding.fontSizeGroup.check(binding.fontNormal.id)
                    prefs.edit { putString("font_size", "normal") }
                    updateAccessibilitySettings()
                    reloadFragments()
                }
            }
            true
        }

        binding.fontLarge.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                handleDoubleClick(v, "Büyük yazı boyutu. Çift tıklayarak seçebilirsiniz.") {
                    binding.fontSizeGroup.check(binding.fontLarge.id)
                    prefs.edit { putString("font_size", "large") }
                    updateAccessibilitySettings()
                    reloadFragments()
                }
            }
            true
        }

        // Yüksek kontrast modu
        binding.contrastSwitch.isChecked = prefs.getBoolean("contrast", false)
        binding.contrastSwitch.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                handleDoubleClick(v, "Yüksek kontrast modu. Çift tıklayarak açıp kapatabilirsiniz.") {
                    binding.contrastSwitch.isChecked = !binding.contrastSwitch.isChecked
                    prefs.edit { putBoolean("contrast", binding.contrastSwitch.isChecked) }
                    updateAccessibilitySettings()
                    reloadFragments()
                }
            }
            true
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

    private fun setupTTS() {
        try {
            tts = TextToSpeech(requireContext().applicationContext, this)
        } catch (e: Exception) {
            Toast.makeText(context, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
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
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Dil ayarları yapılandırılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
        tts = null
        _binding = null
    }

    fun updateAccessibilitySettings() {
        val prefs = requireContext().getSharedPreferences("settings", 0)
        val contrast = prefs.getBoolean("contrast", false)
        val fontSize = prefs.getString("font_size", "normal")
        val context = requireContext()
        // Renkler
        val bgColor = if (contrast) context.getColor(R.color.high_contrast_black) else context.getColor(R.color.dark_background)
        val textColor = if (contrast) context.getColor(R.color.high_contrast_yellow) else context.getColor(R.color.high_contrast_white)
        val buttonColor = if (contrast) context.getColor(R.color.high_contrast_yellow) else context.getColor(R.color.highlight_button)
        val buttonTextColor = if (contrast) context.getColor(R.color.high_contrast_black) else context.getColor(R.color.high_contrast_black)
        val switchOnColor = if (contrast) context.getColor(R.color.high_contrast_yellow) else context.getColor(R.color.highlight_button)
        val switchOffColor = context.getColor(R.color.high_contrast_white)

        // Arka plan
        binding.root.setBackgroundColor(bgColor)
        // Başlık ve metinler
        binding.settingsTitle.setTextColor(textColor)
        binding.fontSizeTitle.setTextColor(textColor)
        binding.vibrationSwitch.setTextColor(textColor)
        binding.contrastSwitch.setTextColor(textColor)
        binding.fontNormal.setTextColor(textColor)
        binding.fontLarge.setTextColor(textColor)
        // Butonlar
        binding.feedbackButton.setBackgroundColor(buttonColor)
        binding.feedbackButton.setTextColor(buttonTextColor)
        // Switch'ler
        binding.contrastSwitch.trackTintList = ColorStateList.valueOf(if (binding.contrastSwitch.isChecked) switchOnColor else switchOffColor)
        binding.contrastSwitch.thumbTintList = ColorStateList.valueOf(if (binding.contrastSwitch.isChecked) buttonTextColor else switchOffColor)
        binding.vibrationSwitch.trackTintList = ColorStateList.valueOf(if (binding.vibrationSwitch.isChecked) switchOnColor else switchOffColor)
        binding.vibrationSwitch.thumbTintList = ColorStateList.valueOf(if (binding.vibrationSwitch.isChecked) buttonTextColor else switchOffColor)

        // Yazı boyutu
        val scale = if (fontSize == "large") 1.35f else 1.0f
        binding.settingsTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f * scale)
        binding.feedbackButton.textSize = 20f * scale
        binding.vibrationSwitch.textSize = 20f * scale
        binding.fontSizeTitle.textSize = 20f * scale
        binding.fontNormal.textSize = 16f * scale
        binding.fontLarge.textSize = 16f * scale
        binding.contrastSwitch.textSize = 20f * scale

        // View'ı yeniden çizdir
        binding.root.invalidate()
        requireActivity().window.decorView.invalidate()
    }

    private fun reloadFragments() {
        val fm = requireActivity().supportFragmentManager
        fm.fragments.forEach {
            if (it is com.bilocan.mapsforeveryone.ui.favorites.FavoritesFragment ||
                it is com.bilocan.mapsforeveryone.ui.settings.SettingsFragment) {
                fm.beginTransaction().detach(it).attach(it).commitNowAllowingStateLoss()
            }
        }
    }
}