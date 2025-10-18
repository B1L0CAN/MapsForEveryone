package com.bilocan.mapsforeveryone.ui.register

import android.content.Context
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.api.AuthRepository
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog

class RegisterFragment : Fragment() {
    private var passwordVisible = false
    private var confirmPasswordVisible = false
    private lateinit var authRepository: AuthRepository
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        authRepository = AuthRepository(requireContext())
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val contrast = prefs.getBoolean("contrast", false)
        
        val root = view.findViewById<View>(R.id.registerRoot)
        val title = view.findViewById<TextView>(R.id.registerTitle)
        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        val togglePasswordVisibility = view.findViewById<Button>(R.id.togglePasswordVisibility)
        val toggleConfirmPasswordVisibility = view.findViewById<Button>(R.id.toggleConfirmPasswordVisibility)
        registerButton = view.findViewById(R.id.registerButton)
        val backToLoginButton = view.findViewById<Button>(R.id.backToLoginButton)
        
        // Başlangıçta şifreler gizli olmalı
        passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        
        // Başlangıç ikonlarını ayarla (şifre gizliyken üstü çizili göz ikonu)
        togglePasswordVisibility.setBackgroundResource(R.drawable.ic_visibility_off)
        toggleConfirmPasswordVisibility.setBackgroundResource(R.drawable.ic_visibility_off)
        
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
        
        // Şifre onay görünürlüğünü değiştir
        toggleConfirmPasswordVisibility.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            if (confirmPasswordVisible) {
                // Şifreyi göster
                confirmPasswordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleConfirmPasswordVisibility.setBackgroundResource(R.drawable.ic_visibility)
            } else {
                // Şifreyi gizle
                confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleConfirmPasswordVisibility.setBackgroundResource(R.drawable.ic_visibility_off)
            }
            // İmleci metnin sonuna getir
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
        }

        if (contrast) {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            title.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            nameEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            emailEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            passwordEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            confirmPasswordEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            registerButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            registerButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            backToLoginButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            backToLoginButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
        } else {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_background))
            title.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            nameEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            emailEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            passwordEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            confirmPasswordEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            registerButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.highlight_button))
            registerButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
            backToLoginButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.highlight_button))
            backToLoginButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
        }

        registerButton.setOnClickListener {
            registerUser()
        }

        backToLoginButton.setOnClickListener {
            // Giriş ekranına geri dön
            findNavController().navigateUp()
            
            // Kayıt ekranından giriş ekranına döndüğümüzde sesli bildirim yap
            // 300ms gecikme ile çağıralım ki ekran geçişi tamamlansın
            view.postDelayed({
                (activity as? com.bilocan.mapsforeveryone.MainActivity)?.speak("Giriş yapma ekranı")
            }, 300)
        }
    }
    
    private fun registerUser() {
        try {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            
            // Giriş bilgilerini kontrol et
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Şifrelerin eşleştiğini kontrol et
            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Email format kontrolü
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Lütfen geçerli bir e-posta adresi girin", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Şifre uzunluğu kontrolü
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Şifre en az 6 karakter uzunluğunda olmalıdır", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Kayıt butonunu devre dışı bırak
            registerButton.isEnabled = false
            registerButton.text = "Kayıt yapılıyor..."
            
            // Kayıt işlemini yap
            lifecycleScope.launch {
                try {
                    Log.d("RegisterFragment", "Kayıt isteği gönderiliyor - Email: $email, Name: $name")
                    val result = authRepository.register(name, email, password)
                    
                    result.onSuccess { response ->
                        // Fragment'in hala bağlı olup olmadığını kontrol et
                        if (!isAdded) {
                            Log.e("RegisterFragment", "Fragment artık context'e bağlı değil")
                            return@onSuccess
                        }
                        
                        // Debug için - Tüm yanıtı logla
                        Log.d("RegisterFragment", "Register yanıtı tam: $response")
                        
                        // Kayıt başarılı kabul edilir (HTTP 200 OK alındı) - artık başarı durumunu kontrol etmeye gerek yok
                        // userId veya user_id varsa da başarılı kabul et
                        val hasUserId = !response.userId.isNullOrEmpty() || !response.user_id.isNullOrEmpty()
                        Log.d("RegisterFragment", "Kullanıcı ID mevcut mu: $hasUserId")
                        
                        // Kayıt başarılı, giriş ekranına dön
                        Toast.makeText(requireContext(), "Kayıt başarılı! Giriş yapabilirsiniz.", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                        
                    }.onFailure { exception ->
                        // Fragment'in hala bağlı olup olmadığını kontrol et
                        if (!isAdded) {
                            Log.e("RegisterFragment", "Fragment artık context'e bağlı değil")
                            return@onFailure
                        }
                        
                        // Hata durumu
                        Log.e("RegisterFragment", "Kayıt hatası: ${exception.message}", exception)
                        
                        // Daha anlaşılır hata mesajı
                        val errorMessage = exception.message ?: "Bilinmeyen bir hata oluştu"
                        
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
                        
                        registerButton.isEnabled = true
                        registerButton.text = "Kayıt Ol"
                    }
                } catch (e: Exception) {
                    Log.e("RegisterFragment", "Kayıt hatası: ${e.message}", e)
                    
                    // Fragment'in hala bağlı olup olmadığını kontrol et
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Kayıt yapılırken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                        registerButton.isEnabled = true
                        registerButton.text = "Kayıt Ol"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RegisterFragment", "Kayıt hatası: ${e.message}", e)
            Toast.makeText(requireContext(), "Kayıt yapılırken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            registerButton.isEnabled = true
            registerButton.text = "Kayıt Ol"
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.bilocan.mapsforeveryone.MainActivity)?.binding?.bottomNavigationView?.visibility = View.GONE
        
        // Sesli bildirim - ekran açıldığında "Kayıt ekranı" desin
        (activity as? com.bilocan.mapsforeveryone.MainActivity)?.speak("Kayıt ekranı")
    }
} 