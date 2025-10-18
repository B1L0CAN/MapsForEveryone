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
            microphoneButton = binding.microphoneButton
            infoText = binding.infoText
            recognizedText = binding.recognizedText
            
            setupTTS()
            setupViews()
            applyContrastBackground()
            
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            transitRepository = TransitRepository(requireContext())
            
            // Harita üzerindeki çıkış butonunu ayarla
            binding.mapLogoutButton.setOnClickListener {
                handleDoubleClick(it, "Çıkış yapma butonu. Çıkış yapmak için çift tıklayın.") {
                    (activity as? com.bilocan.mapsforeveryone.MainActivity)?.logout()
                }
            }
            
            // Arama butonunu ayarla
            binding.searchButton.setOnClickListener {
                handleDoubleClick(it, "Arama butonu. Çift tıklayarak adres arayabilirsiniz.") {
                    toggleSearchInput()
                }
            }
            
            // Arama kutusu için giriş eylemi
            binding.searchEditText.setOnEditorActionListener { textView, actionId, event ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
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
            
            // Google Maps fragmentini başlat
            val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
            
            // Takip modunu gösteren bir buton ekle
            binding.root.post {
                addFollowLocationButton()
            }
            
            val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            val contrast = prefs.getBoolean("contrast", false)
            
            // Tema renklerini ayarla
            if (contrast) {
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_black))
                binding.infoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
                binding.recognizedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_yellow))
            } else {
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_background))
                binding.infoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
                binding.recognizedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.high_contrast_white))
            }
            
            // Eğer favorilerden adres ile gelindiyse
            arguments?.getString("navigate_to_address")?.let { address ->
                // Direkt navigasyonu başlat (harita hazır olunca başlatılacak)
                (activity as? com.bilocan.mapsforeveryone.MainActivity)?.pendingAddress = address
                
                // Eğer bulunduğumuz konumu alabilirsek, transit API'yi çağıracağız
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
        } catch (e: Exception) {
            Log.e("HomeFragment", "onViewCreated hatası: ${e.message}", e)
            Toast.makeText(context, "Ana sayfa yüklenirken bir hata oluştu", Toast.LENGTH_SHORT).show()
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
            googleMap = map
        setupMap()
        
        // Kullanıcının konumunu al ve haritada göster
        if (hasLocationPermission()) {
            showMyLocation()
            
            // Konum takip modunu ayarla
            googleMap?.setOnCameraMoveStartedListener { reason ->
                // Eğer kullanıcı haritayı manuel hareket ettirirse takip modunu kapat
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isFollowingUser = false
                    updateFollowButtonState()
        }
    }

            // Periyodik konum güncellemelerini başlat
            startLocationUpdates()
            
            // Eğer bekleyen bir adres navigasyonu varsa, başlat
            (activity as? com.bilocan.mapsforeveryone.MainActivity)?.pendingAddress?.let { address ->
                val addressName = (activity as? com.bilocan.mapsforeveryone.MainActivity)?.pendingAddressName ?: "Hedef"
                navigateToAddress(address, addressName)
                
                // Kullanılmış olduğu için bekleyen adresi temizle
                (activity as? com.bilocan.mapsforeveryone.MainActivity)?.pendingAddress = null
                (activity as? com.bilocan.mapsforeveryone.MainActivity)?.pendingAddressName = null
            }
        } else {
            requestLocationPermission()
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
     * İki nokta arasındaki transit rotasını alır ve haritada gösterir
     */
    private fun getTransitRoute(origin: String, destination: String) {
        // Varolan rotaları temizle
        clearRoutePolylines()
        
        // İşlemi başlat
        currentTransitJob?.cancel()
        currentTransitJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                // Yükleniyor bilgisi göster
                infoText?.text = "Rota hesaplanıyor..."
                speak("Rota hesaplanıyor")
                
                // Transit API'yi çağır
                val result = transitRepository.getTransitRoute(origin, destination)
                
                if (result.isSuccess) {
                    val transitResponse = result.getOrNull()
                    if (transitResponse != null) {
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
                        
                        // Rota özeti bilgileri
                        val routeSummary = if (fareInfo.isNotEmpty()) {
                            "Rota bulundu. Tahmini varış süresi $totalDuration, mesafe $totalDistance, ücret $fareInfo"
                        } else {
                            "Rota bulundu. Tahmini varış süresi $totalDuration, mesafe $totalDistance"
                        }
                        
                        // İlk adımı sesli oku
                        if (routeSteps.isNotEmpty()) {
                            speak("$routeSummary. ${routeSteps.first()}")
                        } else {
                            speak(routeSummary)
                        }
                    } else {
                        infoText?.text = "Rota bulunamadı"
                        speak("Rota bulunamadı")
                    }
                } else {
                    // Hata durumunda kullanıcıya bilgi ver
                    val errorMessage = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                    infoText?.text = errorMessage
                    speak("Rota hesaplanırken bir hata oluştu: $errorMessage")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Transit rota hatası: ${e.message}", e)
                infoText?.text = "Rota hesaplanırken bir hata oluştu"
                speak("Rota hesaplanırken bir hata oluştu")
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
                            
                            "Adım $stepNumber: $departureStop durağından $vehicleInfo ile $arrivalStop durağına gidin. ${if (stopsText.isNotEmpty()) "($stopsText)" else ""}"
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
                // Marker ve polyline'ları ekleyebilmek için haritanın hazır olduğundan emin ol
                
                // Adımları belirle - hem steps hem route kontrol et
                val steps = if (!transitResponse.steps.isNullOrEmpty()) {
                    // Öncelikle steps kullan
                    Log.d("HomeFragment", "steps kullanılıyor, ${transitResponse.steps?.size} adım")
                    transitResponse.steps!!
                } else if (transitResponse.route.isNotEmpty()) {
                    // Eğer steps boşsa route kullan
                    Log.d("HomeFragment", "route kullanılıyor, ${transitResponse.route.size} adım")
                    transitResponse.route
                } else {
                    // Hiçbir adım yoksa, boş liste döndür
                    Log.e("HomeFragment", "Rotada adım bulunamadı!")
                    emptyList()
                }
                
                // Hiç adım yoksa, bilgi ver ve çık
                if (steps.isEmpty()) {
                    infoText?.text = "Rotada adım bulunamadı"
                    speak("Rotada adım bulunamadı")
                    return
                }
                
                // Tüm adımları işle ve haritada göster
                val boundsBuilder = LatLngBounds.Builder()
                
                var totalPolylines = 0
                
                for (step in steps) {
                    try {
                        // Adım için konum bilgilerini belirle
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
                        
                        // Polyline için konum noktaları
                        val polylinePoints = ArrayList<com.google.android.gms.maps.model.LatLng>()
                        polylinePoints.add(startLocation)
                        polylinePoints.add(endLocation)
                        
                        // Polyline rengi - adım tipine göre belirle
                        val polylineColor = when (step.type) {
                            "WALKING" -> Color.GRAY
                            "TRANSIT" -> Color.BLUE
                            "BUS" -> Color.BLUE
                            "SUBWAY" -> Color.RED
                            "TRAM" -> Color.GREEN
                            "TRAIN" -> Color.YELLOW
                            else -> Color.BLACK
                        }
                        
                        // Polyline çiz - API'den polyline değil sadece başlangıç/bitiş noktaları geldiğinde
                        val polyline = map.addPolyline(
                            PolylineOptions()
                                .addAll(polylinePoints)
                                .color(polylineColor)
                                .width(8f)
                        )
                        
                        // Çizilen polyline'ı listeye ekle (daha sonra temizleyebilmek için)
                        currentPolylines.add(polyline)
                        totalPolylines++
                        
                        // Bounds'a ekle
                        boundsBuilder.include(startLocation)
                        boundsBuilder.include(endLocation)
                        
                        // Transit bilgisi varsa, durakları işaretle
                        step.transitDetails?.let { details ->
                            // Kalkış durağı
                            val departureLatLng = com.google.android.gms.maps.model.LatLng(
                                details.departureStop.location.lat,
                                details.departureStop.location.lng
                            )
                            
                            map.addMarker(
                                MarkerOptions()
                                    .position(departureLatLng)
                                    .title(details.departureStop.name)
                                    .snippet("Kalkış: ${details.departureTime}")
                            )
                            
                            // Varış durağı
                            val arrivalLatLng = com.google.android.gms.maps.model.LatLng(
                                details.arrivalStop.location.lat,
                                details.arrivalStop.location.lng
                            )
                            
                            map.addMarker(
                                MarkerOptions()
                                    .position(arrivalLatLng)
                                    .title(details.arrivalStop.name)
                                    .snippet("Varış: ${details.arrivalTime}")
                            )
                            
                            // Transit durakları için de bounds'a ekle
                            boundsBuilder.include(departureLatLng)
                            boundsBuilder.include(arrivalLatLng)
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Adım gösterme hatası: ${e.message}", e)
                    }
                }
                
                Log.d("HomeFragment", "Toplam çizilen polyline: $totalPolylines")
                
                // Tüm rotayı haritada görünür yap
                try {
                    val bounds = boundsBuilder.build()
                    val padding = 100 // px
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    map.animateCamera(cameraUpdate)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Harita sınırları oluşturulamadı: ${e.message}", e)
                }
                
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
            // Geocoder kullanarak adresi koordinatlara çevir
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Yükleniyor bilgisi
                    infoText?.text = "Adres aranıyor..."
                    speak("$addressName adresine rota hesaplanıyor")
                    
                    val currentLocation = getCurrentLocationSync()
                    if (currentLocation != null) {
                        // Bulunduğumuz konum ile hedef arasındaki transit rotayı al
                        val originLatLng = "${currentLocation.latitude},${currentLocation.longitude}"
                        getTransitRoute(originLatLng, address)
                    } else {
                        // Konum alınamadı, sadece hedefi haritada göster
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
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Adres arama hatası: ${e.message}", e)
                    infoText?.text = "Adres aranırken bir hata oluştu"
                    speak("Adres aranırken bir hata oluştu")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "navigateToAddress hatası: ${e.message}", e)
            Toast.makeText(context, "Navigasyon başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
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
} 