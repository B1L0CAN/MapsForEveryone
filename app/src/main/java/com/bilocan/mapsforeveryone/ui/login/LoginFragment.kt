package com.bilocan.mapsforeveryone.ui.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.api.AuthRepository
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var passwordVisible = false
    private lateinit var authRepository: AuthRepository
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberMeSwitch: SwitchCompat
    private lateinit var loginButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // AuthRepository'yi kontrollü bir şekilde oluştur
        try {
            authRepository = AuthRepository(requireContext())
        } catch (e: Exception) {
            Log.e("LoginFragment", "AuthRepository oluşturma hatası: ${e.message}", e)
        }
        
        // Konum servisinin açık olup olmadığını kontrol et
        checkLocationEnabled()
        
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val contrast = prefs.getBoolean("contrast", false)
        
        val root = view.findViewById<View>(R.id.loginRoot)
        val welcomeText = view.findViewById<TextView>(R.id.welcomeText)
        val title = view.findViewById<TextView>(R.id.loginTitle)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        val togglePasswordVisibility = view.findViewById<Button>(R.id.togglePasswordVisibility)
        rememberMeSwitch = view.findViewById(R.id.rememberMeSwitch)
        val rememberMeText = view.findViewById<TextView>(R.id.rememberMeText)
        loginButton = view.findViewById(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)
        
        // Başlangıçta şifre gizli olmalı
        passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        
        // Başlangıç ikonunu ayarla (şifre gizliyken üstü çizili göz ikonu)
        togglePasswordVisibility.setBackgroundResource(R.drawable.ic_visibility_off)
        
        // Şifre görünürlüğünü değiştir
        togglePasswordVisibility.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                // Şifreyi göster
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordVisibility.setBackgroundResource(R.drawable.ic_visibility)
            } else {
                // Şifreyi gizle
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordVisibility.setBackgroundResource(R.drawable.ic_visibility_off)
            }
            // İmleci metnin sonuna getir
            passwordEditText.setSelection(passwordEditText.text.length)
        }
        
        // Beni hatırla durumunu yükle
        val rememberMe = prefs.getBoolean("remember_me", false)
        rememberMeSwitch.isChecked = rememberMe
        
        // Thumb (yuvarlak kısım) rengini siyah yap
        rememberMeSwitch.thumbTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_black)

        if (contrast) {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            welcomeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            title.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            loginButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            loginButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            emailEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            passwordEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            rememberMeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            registerButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            registerButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            
            // Switch açıkken sarı, kapalıyken beyaz olacak şekilde ayarla
            // Checked (açık) durumunda track
            rememberMeSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_yellow)
                } else {
                    rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_white)
                }
            }
            
            // Başlangıçta kontrol et
            if (rememberMeSwitch.isChecked) {
                rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_yellow)
            } else {
                rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_white)
            }
        } else {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_background))
            welcomeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            title.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            loginButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.highlight_button))
            loginButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            emailEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            passwordEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            rememberMeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            registerButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.highlight_button))
            registerButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            
            // Switch açıkken sarı, kapalıyken beyaz olacak şekilde ayarla
            rememberMeSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.highlight_button)
                } else {
                    rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_white)
                }
            }
            
            // Başlangıçta kontrol et
            if (rememberMeSwitch.isChecked) {
                rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.highlight_button)
            } else {
                rememberMeSwitch.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.high_contrast_white)
            }
        }

        loginButton.setOnClickListener {
            loginUser()
        }

        registerButton.setOnClickListener {
            // Kayıt sayfasına yönlendirme
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
    
    private fun loginUser() {
        try {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            // Email ve şifre kontrolü
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Email format kontrolü
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Lütfen geçerli bir e-posta adresi girin", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Login butonunu devre dışı bırak
            loginButton.isEnabled = false
            loginButton.text = "Giriş yapılıyor..."
            
            // Giriş işlemini yap
            lifecycleScope.launch {
                try {
                    Log.d("LoginFragment", "Login isteği gönderiliyor - Email: $email")
                    val result = authRepository.login(email, password)
                    
                    result.onSuccess { response ->
                        // Fragmentin hala bağlı olup olmadığını kontrol et
                        if (!isAdded) {
                            Log.e("LoginFragment", "Fragment artık context'e bağlı değil")
                            return@onSuccess
                        }
                        
                        // Debug için - Tüm yanıtı logla
                        Log.d("LoginFragment", "Login yanıtı tam: $response")
                        
                        // Giriş her zaman başarılı kabul edilir (HTTP 200 OK alındı) - Bu noktada buna ulaştığımızda başarılı
                        // Token varsa da başarılı
                        val hasToken = !response.token.isNullOrEmpty()
                        Log.d("LoginFragment", "Token mevcut mu: $hasToken")
                        
                        // Token ve kullanıcı bilgilerini kaydet
                        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                        val userId = response.userId ?: response.user_id ?: "1" // Varsayılan olarak 1
                        
                        prefs.edit()
                            .putBoolean("is_logged_in", true)
                            .putBoolean("remember_me", rememberMeSwitch.isChecked)
                            .putString("user_token", response.token)
                            .putString("user_id", userId)
                            .apply()
                        
                        // Ana ekrana yönlendir
                        activity?.let { mainActivity ->
                            if (mainActivity is com.bilocan.mapsforeveryone.MainActivity) {
                                mainActivity.onUserLoggedIn()
                            }
                        }
                        
                        // Fragment hala bağlıysa Toast göster
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Giriş başarılı", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { exception ->
                        // Fragmentin hala bağlı olup olmadığını kontrol et
                        if (!isAdded) {
                            Log.e("LoginFragment", "Fragment artık context'e bağlı değil")
                            return@onFailure
                        }
                        
                        // Hata durumu - AuthRepository'den gelen direkt mesajı hiç değiştirmeden göster
                        val errorMessage = exception.message ?: "Bilinmeyen bir hata oluştu"
                        Log.e("LoginFragment", "Login hatası: $errorMessage")
                        
                        // JWT hatası için daha görünür bir uyarı
                        if (errorMessage.contains("kimlik doğrulama sorunu") || 
                            errorMessage.contains("Sunucu kimlik doğrulama")) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Sunucu Hatası")
                                .setMessage("Sunucu kimlik doğrulama sorunu yaşanıyor. Bu bir uygulama sorunu değil, sunucu yapılandırma hatasıdır. Lütfen uygulama yöneticinize başvurun.")
                                .setPositiveButton("Tamam", null)
                                .show()
                        } else {
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        }
                        
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Giriş yapma hatası: ${e.message}", e)
                    
                    // Fragmentin hala bağlı olup olmadığını kontrol et
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Giriş yapılırken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Giriş yapma hatası: ${e.message}", e)
            Toast.makeText(requireContext(), "Giriş yapılırken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            loginButton.isEnabled = true
            loginButton.text = "Giriş Yap"
        }
    }
    
    private fun checkLocationEnabled() {
        try {
            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                                   
            if (!isLocationEnabled) {
                showLocationPromptDialog()
            } else {
                // Konum açıksa, konum izni kontrolü yap
                checkLocationPermission()
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Konum kontrolü hatası: ${e.message}", e)
        }
    }
    
    private fun showLocationPromptDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konum Servisi Kapalı")
            .setMessage("Uygulama için konum servisinin açık olması gerekiyor. Konum servisini açmak ister misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                // Konum ayarlarına yönlendir
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Hayır") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "Bazı özellikler konum servisi olmadan çalışmayabilir", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun checkLocationPermission() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.bilocan.mapsforeveryone.MainActivity)?.binding?.bottomNavigationView?.visibility = View.GONE
        
        // AuthRepository'yi tekrar oluştur
        try {
            authRepository = AuthRepository(requireContext())
        } catch (e: Exception) {
            Log.e("LoginFragment", "AuthRepository yeniden oluşturma hatası: ${e.message}", e)
        }
        
        // Sesli bildirim için özel durum kontrolü - ana sayfadan geldiğimizde söyle, ilk açılışta MainActivity söyleyecek
        if (activity?.intent?.hasExtra("FROM_HOME") == true) {
            (activity as? com.bilocan.mapsforeveryone.MainActivity)?.speak("Giriş yapma ekranı")
        }
        
        // Uygulama tekrar öne geldiğinde konum servisi durumunu kontrol et
        checkLocationEnabled()
    }
} 