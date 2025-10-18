package com.bilocan.mapsforeveryone.ui.home

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.databinding.FragmentHomeBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Context
import android.view.Gravity
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bilocan.mapsforeveryone.api.TransitRepository
import com.bilocan.mapsforeveryone.api.model.TransitResponse
import com.bilocan.mapsforeveryone.api.model.RouteStep
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import android.graphics.Color
import kotlin.coroutines.resume
import android.widget.FrameLayout
import android.os.Handler
import android.os.Looper

class HomeFragment : Fragment(), OnInitListener, OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var lastClickTime = 0L
    private var lastClickedView: View? = null
    private var googleMap: GoogleMap? = null
    private val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
    private var currentPolyline: Polyline? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: LatLng? = null
    private var microphoneButton: ImageButton? = null
    private var infoText: TextView? = null
    private var recognizedText: TextView? = null
    private lateinit var transitRepository: TransitRepository
    private var currentTransitJob: Job? = null
    private var currentPolylines = mutableListOf<Polyline>()
    private var isFollowingUser = true
    private var locationUpdateJob: Job? = null
    private var followButton: ImageButton? = null
    private var mapFragment: SupportMapFragment? = null
    private var pendingNavigationIntent: Pair<String, String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            
        try {
            // TTS servisini başlat
            setupTTS()
            
            // MapView initialize et
            mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
            mapFragment?.getMapAsync(this)
            
            // View elemanlarına referans al
            infoText = view.findViewById(R.id.infoText)
            recognizedText = view.findViewById(R.id.recognizedText)
            
            // Konum servisi hazırla
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            
            // Transit repository oluştur
            transitRepository = TransitRepository(requireContext())
            
            // Butonları ayarla
            setupButtons()
            
            // Intent argümanlarını kontrol et - navigasyon isteği varsa
            arguments?.let { args ->
                if (args.containsKey("navigate_to_address")) {
                    val address = args.getString("navigate_to_address")
                    val addressName = args.getString("navigate_name") ?: "Hedef"
                    
                    if (!address.isNullOrEmpty()) {
                        // Harita hazır olmadığından bunu başlangıçta değil, harita hazır olduğunda yapmalıyız
                        // Harita yüklendiğinde tetiklenecek şekilde saklayalım
                        pendingNavigationIntent = Pair(address, addressName)
                        
                        // Yükleniyor bilgisi göster
                        infoText?.text = "Harita yükleniyor, lütfen bekleyin..."
                        speak("Harita yükleniyor, $addressName adresine rota hazırlanıyor")
                    }
                    
                    // Argümanları kullandıktan sonra temizle, tekrar tetiklenmesini önle
                    args.remove("navigate_to_address")
                    args.remove("navigate_name")
                }
            }
            
            // MainActivity'den gelen adres olup olmadığını kontrol et
            val mainActivity = activity as? com.bilocan.mapsforeveryone.MainActivity
            if (mainActivity != null && !mainActivity.pendingAddress.isNullOrEmpty()) {
                val address = mainActivity.pendingAddress!!
                val addressName = mainActivity.pendingAddressName ?: "Hedef"
                
                // Harita yüklendiğinde tetiklenecek şekilde saklayalım
                pendingNavigationIntent = Pair(address, addressName)
                
                // Yükleniyor bilgisi göster
                infoText?.text = "Harita yükleniyor, lütfen bekleyin..."
                speak("Harita yükleniyor, $addressName adresine rota hazırlanıyor")
                
                // İşlem tamamlandı, temizle
                mainActivity.pendingAddress = null
                mainActivity.pendingAddressName = null
            }
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "onViewCreated exception: ${e.message}", e)
            Toast.makeText(context, "Harita ekranı yüklenirken bir hata oluştu", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupTTS() {
        try {
            tts = TextToSpeech(requireContext().applicationContext, this)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Ses sentezi başlatma hatası: ${e.message}", e)
            Toast.makeText(context, "Ses sentezi başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViews() {
        try {
            microphoneButton?.setOnClickListener {
                handleDoubleClick(it, "Mikrofon butonu. Çift tıklayarak sesli komut verebilirsiniz.") {
                    if (isTtsInitialized && tts != null) {
                        try {
                            startVoiceRecognition()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ses tanıma başlatılamadı: "+e.message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Ses sentezi hazır değil", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupViews hatası: ${e.message}", e)
        }
    }

    private fun handleDoubleClick(view: View, description: String, action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (lastClickedView == view && currentTime - lastClickTime < 500) {
            action()
            lastClickTime = 0
            lastClickedView = null
        } else {
            speak(description)
            lastClickTime = currentTime
            lastClickedView = view
        }
    }

    private fun startVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Konuşun...")
            
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            Toast.makeText(context, "Ses tanıma başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (results != null && results.isNotEmpty()) {
                val recognizedText = results[0]
                processVoiceCommand(recognizedText)
            }
        }
    }

    private fun processVoiceCommand(command: String) {
        if (isTtsInitialized && tts != null) {
            val dialog = VoiceAddressDialog(
                address = command,
                tts = tts,
                onApprove = { adres ->
                    navigateToAddress(adres)
                },
                onRetry = {
                    startVoiceRecognition()
                }
            )
            dialog.show(parentFragmentManager, "VoiceAddressDialog")
        }
    }

    private fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun applyContrastBackground() {
        try {
            val prefs = requireContext().getSharedPreferences("settings", 0)
            val contrast = prefs.getBoolean("contrast", false)
            val color = if (contrast) requireContext().getColor(R.color.high_contrast_black) else requireContext().getColor(R.color.dark_background)
            binding.root.setBackgroundColor(color)
        } catch (e: Exception) {
            Log.e("HomeFragment", "applyContrastBackground hatası: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val turkishLocale = Locale("tr", "TR")
            val result = tts?.setLanguage(turkishLocale)
            
            when (result) {
                TextToSpeech.LANG_MISSING_DATA -> {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                    Toast.makeText(context, "Türkçe dil paketi yükleniyor...", Toast.LENGTH_LONG).show()
                }
                TextToSpeech.LANG_NOT_SUPPORTED -> {
                    tts?.setLanguage(Locale.getDefault())
                    Toast.makeText(context, "Türkçe desteklenmiyor, varsayılan dil kullanılıyor", Toast.LENGTH_SHORT).show()
                }
                TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                    isTtsInitialized = true
                    
                    // TTS başarıyla başlatıldığında "Ana sayfa" sesli bildirimi artık yapmıyoruz
                    // MainActivity'deki onUserLoggedIn metodunda yapılıyor
                }
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
        locationUpdateJob?.cancel()
        locationUpdateJob = null
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            
            // Harita tipini ayarla
            googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            
            // UI ayarlarını optimize et (kontrolleri devre dışı bırak)
            googleMap?.uiSettings?.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = true
                isRotateGesturesEnabled = true
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
                isTiltGesturesEnabled = false
                isMapToolbarEnabled = false
            }
            
            // Tıklama dinleyicisi ekle
            googleMap?.setOnMapClickListener { latLng ->
                // Eğer takip modu açıksa ve haritaya tıklandıysa, takip modunu kapat
                if (isFollowingUser) {
                    isFollowingUser = false
                    updateFollowButtonState()
                    speak("Konum takibi kapatıldı")
                }
            }
            
            // My Location katmanını etkinleştir (konum izni varsa)
            showMyLocation()
            
            // Harita yüklendi bilgisi ver
            infoText?.text = "Harita hazır"
            speak("Harita hazır")
            
            // Eğer bekleyen bir adres navigasyonu varsa, başlat
            pendingNavigationIntent?.let { (address, addressName) ->
                try {
                    // Harita artık hazır, navigasyonu başlat
                    infoText?.text = "$addressName adresine yönlendiriliyor..."
                    Log.d("HomeFragment", "Bekleyen navigasyon isteği başlatılıyor: $address")
                    
                    // Karıştırıcı temizlemeleri önlemek için bu flag'i kullan
                    val pendingNavigation = pendingNavigationIntent
                    pendingNavigationIntent = null
                    
                    // Küçük bir gecikme ekleyerek haritanın tam olarak hazır olmasını sağla
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (pendingNavigation != null) {
                            val (addr, addrName) = pendingNavigation
                            navigateToAddress(addr, addrName)
                        }
                    }, 500)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Bekleyen navigasyon çalıştırılamadı: ${e.message}", e)
                    infoText?.text = "Navigasyon başlatılamadı"
                    speak("Navigasyon başlatılamadı")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "onMapReady hatası: ${e.message}", e)
            Toast.makeText(context, "Harita hazırlanırken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Haritayı ve ilgili ayarları yapılandır
     */
    private fun setupMap() {
        try {
            googleMap?.let { map ->
                // Map ayarlarını yapılandır
                map.uiSettings.isZoomControlsEnabled = true
                map.uiSettings.isZoomGesturesEnabled = true
                map.uiSettings.isScrollGesturesEnabled = true
                map.uiSettings.isRotateGesturesEnabled = true
                
                // Yüksek kontrastlı mod için harita stilini güncelle
                val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                val contrast = prefs.getBoolean("contrast", false)
                
                if (contrast) {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL)
                    // TODO: Yüksek kontrastlı harita stili eklenebilir
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupMap hatası: ${e.message}", e)
        }
    }

    /**
     * Rota üzerindeki polyline'ları temizler
     */
    private fun clearRoutePolylines() {
        for (polyline in currentPolylines) {
            polyline.remove()
        }
        currentPolylines.clear()
    }
    
    /**
     * Haritayı temizler (tüm markerlar ve rotalar)
     */
    private fun clearMap() {
        try {
            // Tüm polyline'ları temizle
            clearRoutePolylines()
            
            // Tüm markerları temizle
            googleMap?.clear()
            
            // Kullanıcı konumunu göstermeye devam et
            if (hasLocationPermission()) {
                googleMap?.isMyLocationEnabled = true
                googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            }
            
            Log.d("HomeFragment", "Harita temizlendi")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Harita temizlenirken hata: ${e.message}", e)
        }
    }
    
    /**
     * İki nokta arasındaki transit rotasını alır ve haritada gösterir
     */
    private fun getTransitRoute(origin: String, destination: String) {
        // Haritayı temizle (tüm rotalar ve markerlar)
        clearMap()
        
        // İşlemi başlat
        currentTransitJob?.cancel()
        currentTransitJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Yükleniyor bilgisi göster
                infoText?.text = "Rota hesaplanıyor..."
                speak("Rota hesaplanıyor")
                
                var retryCount = 0
                val maxRetries = 3
                var result: Result<TransitResponse>? = null
                
                while (retryCount < maxRetries) {
                    // Transit API'yi çağır
                    result = transitRepository.getTransitRoute(origin, destination)
                    
                    if (result.isSuccess) {
                        break
                    }
                    
                    val error = result.exceptionOrNull()
                    Log.w("HomeFragment", "Rota denemesi ${retryCount + 1} başarısız: ${error?.message}")
                    
                    // Zaman aşımı hatası ise ve son deneme değilse tekrar dene
                    if (error?.message?.contains("zaman aşımı") == true && retryCount < maxRetries - 1) {
                        retryCount++
                        infoText?.text = "Rota hesaplanıyor... (Deneme ${retryCount + 1}/$maxRetries)"
                        speak("Rota hesaplanıyor, lütfen bekleyin")
                        delay(2000) // 2 saniye bekle
                        continue
                    }
                    
                    break
                }
                
                if (result?.isSuccess == true) {
                    val transitResponse = result.getOrNull()
                    if (transitResponse != null) {
                        // Adım verilerinin boş olup olmadığını kontrol et
                        val hasSteps = !transitResponse.steps.isNullOrEmpty() || transitResponse.route.isNotEmpty()
                        
                        if (hasSteps) {
                            // Rotayı haritada göster
                            displayTransitRoute(transitResponse)
                            
                            // Rotayı gösterdikten sonra, tekrar kullanıcı konumuna odaklan
                            delay(1000) // Haritanın rotayı göstermesi için biraz bekle
                            if (isFollowingUser) {
                                moveCameraToUserLocation()
                            }
                            
                            // Rota adımlarını oluştur
                            val routeSteps = createRouteDirections(transitResponse)
                            
                            // Ekranda ve sesli olarak yol tarifini anlat
                            val stepsDescription = routeSteps.joinToString("\n")
                            recognizedText?.text = stepsDescription
                            
                            // Sesli bilgilendirme ve ekran bilgisi için değerleri belirle
                            var totalDuration = ""
                            var totalDistance = ""
                            var fareInfo = ""
                            
                            // Öncelikli olarak summary alanından bilgileri al
                            if (transitResponse.summary != null) {
                                totalDuration = transitResponse.summary.totalDuration
                                totalDistance = transitResponse.summary.totalDistance
                                fareInfo = transitResponse.summary.fare
                            } 
                            // Eğer summary yoksa veya boşsa, doğrudan alanları kontrol et
                            else {
                                totalDuration = transitResponse.totalDuration
                                totalDistance = transitResponse.totalDistance
                            }
                            
                            // Değerler hala boşsa uygun mesaj göster
                            if (totalDuration.isEmpty() && totalDistance.isEmpty()) {
                                infoText?.text = "Varış bilgisi alınamadı"
                                speak("Rota bulundu ancak varış bilgisi alınamadı")
                                return@launch
                            }
                            
                            // Ekranda bilgi göster
                            infoText?.text = if (fareInfo.isNotEmpty()) {
                                "Varış: $totalDuration, $totalDistance, $fareInfo"
                            } else {
                                "Varış: $totalDuration, $totalDistance"
                            }
                            
                            // İlk adımı veya aktarma bilgisini sesli oku
                            val firstStep = transitResponse.steps?.firstOrNull() ?: transitResponse.route.firstOrNull()
                            if (firstStep != null) {
                                val firstStepDescription = when (firstStep.type) {
                                    "WALKING" -> "İlk adım: ${firstStep.instruction}"
                                    "TRANSIT" -> {
                                        val transitDetails = firstStep.transitDetails
                                        if (transitDetails != null) {
                                            val vehicleType = when(transitDetails.line.vehicle) {
                                                "BUS" -> "otobüs"
                                                "SUBWAY" -> "metro"
                                                "TRAM" -> "tramvay"
                                                "TRAIN" -> "tren"
                                                else -> "toplu taşıma"
                                            }
                                            
                                            val lineInfo = if (transitDetails.line.shortName.isNotEmpty()) {
                                                "${transitDetails.line.shortName} numaralı $vehicleType"
                                            } else {
                                                vehicleType
                                            }
                                            
                                            "İlk adım: ${transitDetails.departureStop.name} durağından $lineInfo ile ${transitDetails.arrivalStop.name} durağına gidin."
                                        } else {
                                            "İlk adım: ${firstStep.instruction}"
                                        }
                                    }
                                    else -> "İlk adım: ${firstStep.instruction}"
                                }
                                speak(firstStepDescription)
                            }
                        } else {
                            infoText?.text = "Rota bulunamadı"
                            speak("Bu güzergah için toplu taşıma rotası bulunamadı")
                        }
                    }
                } else {
                    // Hata durumunda kullanıcıya bilgi ver
                    val errorMessage = result?.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                    Log.e("HomeFragment", "Rota hesaplanırken hata: $errorMessage")
                    Log.e("HomeFragment", "Rota parametreleri: origin=$origin, destination=$destination")
                    
                    // Kullanıcıya daha açıklayıcı bilgi ver
                    val userErrorMessage = when {
                        errorMessage.contains("zaman aşımı") -> 
                            "Rota hesaplama süresi uzadı. Lütfen şunları deneyin:\n" +
                            "1. Daha spesifik bir adres girin\n" +
                            "2. Daha kısa bir mesafe seçin\n" +
                            "3. Birkaç dakika sonra tekrar deneyin"
                        errorMessage.contains("found") || errorMessage.contains("bulunamadı") ->
                            "Girdiğiniz adres bulunamadı. Lütfen farklı bir adres deneyin veya daha açık bir şekilde belirtin."
                        errorMessage.contains("network") || errorMessage.contains("internet") || errorMessage.contains("bağlantı") ->
                            "İnternet bağlantınızda bir sorun olabilir. Lütfen bağlantınızı kontrol edin ve tekrar deneyin."
                        errorMessage.contains("ZERO_RESULTS") || errorMessage.contains("sıfır sonuç") ->
                            "Bu güzergah için toplu taşıma rotası bulunamadı. Lütfen farklı bir adres deneyin veya daha bilinen bir yer adı kullanın."
                        else -> "Rota bulunamadı. Lütfen daha yaygın bir adres veya yer adı girin."
                    }
                    
                    infoText?.text = userErrorMessage
                    speak(userErrorMessage)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Transit rota hatası: ${e.message}", e)
                infoText?.text = "Rota hesaplanırken bir hata oluştu"
                speak("Rota hesaplanırken bir hata oluştu, lütfen tekrar deneyin")
            }
        }
    }
    
    /**
     * Transit rotasından okunabilir yol tarifleri oluşturur
     */
    private fun createRouteDirections(transitResponse: TransitResponse): List<String> {
        val directions = mutableListOf<String>()
        
        // Adımları belirle
        val steps = if (!transitResponse.steps.isNullOrEmpty()) {
            transitResponse.steps!!
        } else if (transitResponse.route.isNotEmpty()) {
            transitResponse.route
        } else {
            emptyList()
        }
        
        // Öncelikle rota özetini ekleyelim
        val totalDuration = transitResponse.summary?.totalDuration ?: transitResponse.totalDuration
        val totalDistance = transitResponse.summary?.totalDistance ?: transitResponse.totalDistance
        val fareInfo = transitResponse.summary?.fare ?: ""
        
        // Rota özeti ekle
        val summaryText = StringBuilder("Rota özeti: ")
        if (totalDistance.isNotEmpty()) summaryText.append("Mesafe $totalDistance, ")
        if (totalDuration.isNotEmpty()) summaryText.append("Süre $totalDuration")
        if (fareInfo.isNotEmpty()) summaryText.append(", Ücret $fareInfo")
        
        directions.add(summaryText.toString())
        directions.add("") // Boş satır ekle
        
        // Eğer varsa aktarma bilgilerini ekle - aktarma yoksa hiç bilgi gösterme
        transitResponse.transferInfo?.let { transferInfo ->
            if (transferInfo.hasTransfer && transferInfo.transferCount > 0) {
                // Aktarma bilgisini ekle
                when (transferInfo.transferCount) {
                    1 -> directions.add("Bu rotada 1 aktarma yapmanız gerekiyor.")
                    else -> directions.add("Bu rotada toplam ${transferInfo.transferCount} aktarma yapmanız gerekiyor.")
                }
                
                // Aktarma noktalarını ekle
                transferInfo.transferPoints.forEachIndexed { index, transferPoint ->
                    val transferDirection = "Aktarma ${index + 1}: ${transferPoint.location} durağında " +
                            "${transferPoint.fromBus} numaralı araçtan inip ${transferPoint.toBus} numaralı araca bineceksiniz. " +
                            "İniş saati: ${transferPoint.arrivalTime}, Kalkış saati: ${transferPoint.departureTime}, " +
                            "Bekleme süresi: ${transferPoint.waitingTime}."
                    directions.add(transferDirection)
                    
                    // Tek aktarma ise veya son aktarma noktasıysa, daha spesifik bir yönlendirme ekle
                    if (transferInfo.transferCount == 1 || index == transferInfo.transferPoints.size - 1) {
                        directions.add("Aktarma yaparken şunlara dikkat edin:")
                        directions.add("1- ${transferPoint.fromBus} numaralı araçtan indikten sonra durakta tabelaları takip edin.")
                        directions.add("2- ${transferPoint.toBus} numaralı aracın binme durağını bulun.")
                        directions.add("3- Yaklaşık ${transferPoint.waitingTime} bekledikten sonra ${transferPoint.toBus} numaralı araç gelecek.")
                    }
                }
                
                directions.add("") // Boş satır ekle
            }
            // Else durumunda hiçbir şey yapma - aktarma yoksa hiç mesaj gösterme
            
            directions.add("Rota adımlarınız:")
            true // let bloğu için bir değer döndürüyoruz
        } ?: run {
            // transferInfo null ise hiçbir şey yazma
            directions.add("Rota adımlarınız:")
            false // run bloğu için bir değer döndürüyoruz
        }
        
        // Her bir adım için yönlendirme oluştur
        steps.forEachIndexed { index, step ->
            try {
                val stepNumber = index + 1
                val stepDirection = when(step.type) {
                    "WALKING" -> {
                        val distance = step.distance
                        "Adım $stepNumber: ${distance} yürüyün. ${step.instruction}"
                    }
                    "TRANSIT", "BUS", "SUBWAY", "TRAM", "TRAIN" -> {
                        val transitDetails = step.transitDetails
                        if (transitDetails != null) {
                            val lineName = transitDetails.line.name
                            val lineShortName = transitDetails.line.shortName
                            val vehicleType = when(transitDetails.line.vehicle) {
                                "BUS" -> "otobüs"
                                "SUBWAY" -> "metro"
                                "TRAM" -> "tramvay"
                                "TRAIN" -> "tren"
                                else -> "toplu taşıma"
                            }
                            
                            val vehicleInfo = if (lineShortName.isNotEmpty()) {
                                "$lineShortName numaralı $vehicleType"
                            } else {
                                vehicleType
                            }
                            
                            val departureStop = transitDetails.departureStop.name
                            val arrivalStop = transitDetails.arrivalStop.name
                            val numStops = transitDetails.numStops
                            val stopsText = if (numStops > 0) "$numStops durak" else ""
                            
                            val stopInfo = if (stopsText.isNotEmpty()) "($stopsText)" else ""
                            "Adım $stepNumber: $departureStop durağından $vehicleInfo ile $arrivalStop durağına gidin. $stopInfo"
                        } else {
                            "Adım $stepNumber: Toplu taşıma kullanın. ${step.instruction}"
                        }
                    }
                    else -> "Adım $stepNumber: ${step.instruction}"
                }
                
                directions.add(stepDirection)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Rota yönlendirme hatası: ${e.message}", e)
                directions.add("Adım ${index + 1}: Yönlendirme hatası")
            }
        }
        
        return directions
    }
    
    /**
     * Transit rotasını haritada gösterir
     */
    private fun displayTransitRoute(transitResponse: TransitResponse) {
        googleMap?.let { map ->
            try {
                // Önceki tüm polyline ve markerları temizle
                clearRoutePolylines()
                currentPolylines.clear()

                // boundsBuilder her zaman başta tanımlanmalı
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()

                if (!transitResponse.polyline.isNullOrEmpty()) {
                    val route: List<com.google.android.gms.maps.model.LatLng> = com.google.maps.android.PolyUtil.decode(transitResponse.polyline)
                    val polyline = map.addPolyline(
                        com.google.android.gms.maps.model.PolylineOptions()
                            .addAll(route)
                            .color(android.graphics.Color.BLUE)
                            .width(8f)
                    )
                    currentPolylines.add(polyline)
                    route.forEach { boundsBuilder.include(it) }
                } else {
                    val steps = if (!transitResponse.steps.isNullOrEmpty()) {
                        transitResponse.steps!!
                    } else if (transitResponse.route.isNotEmpty()) {
                        transitResponse.route
                    } else {
                        emptyList()
                    }
                    if (steps.isEmpty()) {
                        infoText?.text = "Rotada adım bulunamadı"
                        speak("Rotada adım bulunamadı, lütfen tekrar deneyin")
                        return
                    }
                    for (step in steps) {
                        val startLocation = if (step.walkingDetails != null) {
                            com.google.android.gms.maps.model.LatLng(
                                step.walkingDetails.startLocation.lat,
                                step.walkingDetails.startLocation.lng
                            )
                        } else {
                            com.google.android.gms.maps.model.LatLng(
                                step.startLocation.lat,
                                step.startLocation.lng
                            )
                        }
                        val endLocation = if (step.walkingDetails != null) {
                            com.google.android.gms.maps.model.LatLng(
                                step.walkingDetails.endLocation.lat,
                                step.walkingDetails.endLocation.lng
                            )
                        } else {
                            com.google.android.gms.maps.model.LatLng(
                                step.endLocation.lat,
                                step.endLocation.lng
                            )
                        }
                        val polylinePoints = ArrayList<com.google.android.gms.maps.model.LatLng>()
                        polylinePoints.add(startLocation)
                        polylinePoints.add(endLocation)
                        val polylineColor = when (step.type) {
                            "WALKING" -> android.graphics.Color.GRAY
                            "TRANSIT" -> android.graphics.Color.BLUE
                            "BUS" -> android.graphics.Color.BLUE
                            "SUBWAY" -> android.graphics.Color.RED
                            "TRAM" -> android.graphics.Color.GREEN
                            "TRAIN" -> android.graphics.Color.YELLOW
                            else -> android.graphics.Color.BLACK
                        }
                        val polyline = map.addPolyline(
                            com.google.android.gms.maps.model.PolylineOptions()
                                .addAll(polylinePoints)
                                .color(polylineColor)
                                .width(8f)
                        )
                        currentPolylines.add(polyline)
                        boundsBuilder.include(startLocation)
                        boundsBuilder.include(endLocation)
                    }
                }

                Log.d("HomeFragment", "Toplam çizilen polyline: ${currentPolylines.size}")

                // Kamera sınırlarını ayarla
                try {
                    val bounds = boundsBuilder.build()
                    val padding = 100 // px
                    val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    map.animateCamera(cameraUpdate)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Harita sınırları oluşturulamadı: "+e.message, e)
                }

                // ... Aktarma noktası markerları vs. burada devam edebilir ...
            } catch (e: Exception) {
                Log.e("HomeFragment", "Rota gösterme hatası: ${e.message}", e)
            }
        }
    }
    
    /**
     * Google Maps üzerinde navigasyonu başlatır (adrese göre)
     */
    fun navigateToAddress(address: String, addressName: String = "Hedef") {
        try {
            // Haritayı temizle (önceki rotaları ve markerları kaldır)
            clearMap()
            
            // Geocoder kullanarak adresi koordinatlara çevir
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Yükleniyor bilgisi
                    infoText?.text = "Adres aranıyor..."
                    speak("$addressName adresine rota hesaplanıyor")
                    
                    // Eğer harita yüklenmemişse, kullanıcıya bildir ve çık
                    if (googleMap == null) {
                        Log.e("HomeFragment", "Harita henüz yüklenmedi")
                        infoText?.text = "Harita hazırlanıyor..."
                        speak("Harita hazırlanıyor, lütfen biraz bekleyin")
                        return@launch
                    }
                    
                    val currentLocation = getCurrentLocationSync()
                    if (currentLocation != null) {
                        // Bulunduğumuz konum ile hedef arasındaki transit rotayı al
                        val originLatLng = "${currentLocation.latitude},${currentLocation.longitude}"
                        getTransitRoute(originLatLng, address)
                    } else {
                        // Konum alınamadı, sadece hedefi haritada göster
                        try {
                            // Önce konum dinlemeyi aç, belki alınabilir
                            if (hasLocationPermission()) {
                                googleMap?.isMyLocationEnabled = true
                                googleMap?.uiSettings?.isMyLocationButtonEnabled = false
                            }
                            
                            // Konumu alamadık, haritada sadece hedefi göster
                            withContext(Dispatchers.IO) {
                                val addresses = geocoder.getFromLocationName(address, 1)
                                if (addresses?.isNotEmpty() == true) {
                                    withContext(Dispatchers.Main) {
                                        val location = addresses[0]
                                        val latLng = com.google.android.gms.maps.model.LatLng(
                                            location.latitude,
                                            location.longitude
                                        )
                                        
                                        // Haritada göster
                                        googleMap?.addMarker(
                                            MarkerOptions()
                                                .position(latLng)
                                                .title(addressName)
                                        )
                                        
                                        // Haritayı hedefe doğru hareket ettir
                                        googleMap?.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                        )
                                        
                                        infoText?.text = "Konum gösteriliyor: $addressName"
                                        speak("Konum bulundu. Konumunuz alınamadığı için rota hesaplanamadı.")
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        infoText?.text = "Adres bulunamadı"
                                        speak("Üzgünüm, $addressName adresi bulunamadı")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Geocoding hatası: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                infoText?.text = "Adres aranıyor..."
                                speak("Adres aranıyor, lütfen bekleyin")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "navigateToAddress hatası: ${e.message}", e)
                    infoText?.text = "Rota hesaplanıyor..."
                    speak("Rota hesaplanıyor, lütfen bekleyin")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "navigateToAddress hatası: ${e.message}", e)
            infoText?.text = "Rota hesaplanıyor..."
            speak("Rota hesaplanıyor, lütfen bekleyin")
        }
    }

    /**
     * Konum izninin olup olmadığını kontrol eder
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Konum izni ister
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }
    
    /**
     * Kullanıcının konumunu gösterir
     */
    private fun showMyLocation() {
        try {
            if (hasLocationPermission()) {
                googleMap?.isMyLocationEnabled = true
                
                // My Location butonu gösterme (kendi özel butonumuzu kullanacağız)
                googleMap?.uiSettings?.isMyLocationButtonEnabled = false
                
                // İlk konum gösterimi
                moveCameraToUserLocation()
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "showMyLocation hatası: ${e.message}", e)
        }
    }
    
    /**
     * Kullanıcının güncel konumunu alır
     */
    private fun getCurrentLocation(callback: (android.location.Location?) -> Unit) {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        callback(location)
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Konum alınamadı: ${e.message}", e)
                        callback(null)
                    }
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "getCurrentLocation hatası: ${e.message}", e)
            callback(null)
        }
    }
    
    /**
     * Kullanıcının güncel konumunu senkron olarak alır (coroutine içinde kullanmak için)
     */
    private suspend fun getCurrentLocationSync(): android.location.Location? = suspendCancellableCoroutine { cont ->
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        cont.resume(location) { 
                            Log.e("HomeFragment", "getCurrentLocationSync iptal edildi") 
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Konum alınamadı: ${e.message}", e)
                        cont.resume(null) {
                            Log.e("HomeFragment", "getCurrentLocationSync iptal edildi")
                        }
                    }
            } else {
                cont.resume(null) {
                    Log.e("HomeFragment", "getCurrentLocationSync iptal edildi")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "getCurrentLocationSync hatası: ${e.message}", e)
            if (cont.isActive) {
                cont.resume(null) {
                    Log.e("HomeFragment", "getCurrentLocationSync iptal edildi")
                }
            }
        }
    }

    /**
     * Takip butonunu ekler
     */
    private fun addFollowLocationButton() {
        try {
            followButton = ImageButton(requireContext())
            followButton?.setImageResource(android.R.drawable.ic_menu_mylocation)
            followButton?.id = View.generateViewId()
            
            // Konum takibini aktif/deaktif yapan tıklama işlevini ekle
            followButton?.setOnClickListener {
                isFollowingUser = !isFollowingUser
                if (isFollowingUser) {
                    speak("Konum takibi açıldı")
                    moveCameraToUserLocation()
                } else {
                    speak("Konum takibi kapatıldı")
                }
                updateFollowButtonState()
            }
            
            // Buton stilini ayarla
            followButton?.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button_background)
            followButton?.setPadding(16, 16, 16, 16)
            
            // Margin değerini al
            val margin = resources.getDimensionPixelSize(R.dimen.map_button_margin)
            
            // MapView'ı bul - SupportMapFragment içinden MapView'ı alıyoruz
            val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
            mapFragment?.view?.let { mapView ->
                // Buton için FrameLayout.LayoutParams
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                
                // Konumu ve marjini ayarla - haritanın SOL ÜST köşesine taşı
                layoutParams.gravity = Gravity.TOP or Gravity.START
                layoutParams.setMargins(margin, margin, margin, margin)
                
                // Parametreleri butona uygula
                followButton?.layoutParams = layoutParams
                
                // MapView bir FrameLayout içinde olduğu için, eğer MapView direkt olarak bir ViewGroup ise 
                // buttonu oraya ekleyebiliriz
                if (mapView is ViewGroup) {
                    mapView.addView(followButton)
                } else {
                    // Eğer MapView direkt olarak ViewGroup değilse, parent'ına ekliyoruz
                    (mapView.parent as? ViewGroup)?.addView(followButton)
                }
            }
            
            // İlk durumu ayarla
            updateFollowButtonState()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Takip butonu eklenemedi: ${e.message}", e)
        }
    }
    
    /**
     * Takip butonunun görünümünü günceller
     */
    private fun updateFollowButtonState() {
        try {
            if (isFollowingUser) {
                followButton?.alpha = 1.0f
            } else {
                followButton?.alpha = 0.5f
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Takip butonu güncellenemedi: ${e.message}", e)
        }
    }
    
    /**
     * Periyodik konum güncellemelerini başlatır
     */
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        
        // Mevcut işi iptal et
        locationUpdateJob?.cancel()
        
        // Yeni bir iş başlat
        locationUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    if (isFollowingUser) {
                        moveCameraToUserLocation()
                    }
                    delay(3000) // 3 saniyede bir güncelle
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Konum güncelleme hatası: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Kamerayı kullanıcının konumuna taşır
     */
    private fun moveCameraToUserLocation() {
        if (!hasLocationPermission()) return
        
        try {
            getCurrentLocation { location ->
                if (location != null && isFollowingUser) {
                    val latLng = com.google.android.gms.maps.model.LatLng(
                        location.latitude,
                        location.longitude
                    )
                    
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(latLng, 17f)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Kamera kullanıcıya taşınamadı: ${e.message}", e)
        }
    }

    // Arama kutusunu göster/gizle
    private fun toggleSearchInput() {
        if (binding.searchEditText.visibility == View.VISIBLE) {
            hideSearchInput()
        } else {
            showSearchInput()
        }
    }
    
    // Arama kutusunu göster
    private fun showSearchInput() {
        binding.searchEditText.visibility = View.VISIBLE
        binding.searchEditText.requestFocus()
        // Klavyeyi göster
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    // Arama kutusunu gizle
    private fun hideSearchInput() {
        binding.searchEditText.visibility = View.GONE
        // Klavyeyi gizle
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
    
    // Yazılı adresi ara
    private fun searchAddress(address: String) {
        speak("$address adresi aranıyor")
        if (hasLocationPermission()) {
            getCurrentLocation { location ->
                if (location != null) {
                    val originLatLng = "${location.latitude},${location.longitude}"
                    getTransitRoute(originLatLng, address)
                } else {
                    speak("Konumunuz alınamadı. Lütfen konum izinlerinizi kontrol edin.")
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    /**
     * Harita ekranındaki butonları ayarlar
     */
    private fun setupButtons() {
        try {
            // Arama ve çıkış butonlarını ayarla (eğer binding kullanılıyorsa)
            try {
                // Mikrofon butonu
                binding.microphoneButton?.setOnClickListener {
                    handleDoubleClick(it, "Mikrofon butonu. Çift tıklayarak sesli komut verebilirsiniz.") {
                        startVoiceRecognition()
                    }
                }
                
                // Harita üzerindeki çıkış butonunu ayarla
                binding.mapLogoutButton?.setOnClickListener {
                    handleDoubleClick(it, "Çıkış yapma butonu. Çıkış yapmak için çift tıklayın.") {
                        (activity as? com.bilocan.mapsforeveryone.MainActivity)?.logout()
                    }
                }
                
                // Arama butonunu ayarla
                binding.searchButton?.setOnClickListener {
                    handleDoubleClick(it, "Arama butonu. Çift tıklayarak adres arayabilirsiniz.") {
                        toggleSearchInput()
                    }
                }
                
                // Arama kutusu için giriş eylemi
                binding.searchEditText?.setOnEditorActionListener { textView, actionId, event ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || 
                        actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                        val address = textView.text.toString().trim()
                        if (address.isNotEmpty()) {
                            searchAddress(address)
                            hideSearchInput()
                        }
                        true
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                // View referansları düzgün bağlanamadı, binding null olabilir
                Log.e("HomeFragment", "Buton referansları bağlanamadı: ${e.message}")
            }
            
            // Takip modunu gösteren bir buton ekle (bu işlem harita yüklendiğinde yapılacak)
            try {
                addFollowLocationButton()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Takip butonu eklenemedi: ${e.message}")
            }
            
            // Tema renklerini ayarla
            try {
                val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                val contrast = prefs.getBoolean("contrast", false)
                applyContrastBackground(contrast)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Tema ayarlanamadı: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupButtons hatası: ${e.message}", e)
        }
    }
    
    /**
     * Contrast modunu uygular
     */
    private fun applyContrastBackground(contrast: Boolean = false) {
        try {
            if (contrast) {
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
                binding.infoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
                binding.recognizedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            } else {
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_background))
                binding.infoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
                binding.recognizedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Contrast modu uygulanamadı: ${e.message}")
        }
    }
} 