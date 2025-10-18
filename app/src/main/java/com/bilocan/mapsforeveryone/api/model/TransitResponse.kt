package com.bilocan.mapsforeveryone.api.model

import com.google.gson.annotations.SerializedName

data class TransitResponse(
    val error: Boolean = false,
    
    @SerializedName("status")
    val status: String = "",           // İşlem durumu ("OK" veya "ERROR" gibi)
    
    val message: String = "",          // Hata durumunda mesaj
    val details: String = "",          // Hata detayları
    
    @SerializedName("steps")
    var steps: List<RouteStep>? = ArrayList(),   // Rotadaki adımlar (steps formatında)
    
    @SerializedName("route")
    val route: List<RouteStep> = ArrayList(),   // Rotadaki adımlar (eski format)
    
    @SerializedName("summary")
    val summary: Summary? = null,      // Rota özeti
    
    @SerializedName("transfer_info")
    val transferInfo: TransferInfo? = null, // Aktarma bilgileri
    
    @SerializedName("total_distance")
    val totalDistance: String = "",    // Toplam mesafe (metin formatında, örn: "5.2 km")
    
    @SerializedName("total_duration")
    val totalDuration: String = "",    // Toplam süre (metin formatında, örn: "25 dakika")
    
    @SerializedName("polyline")
    val polyline: String = "",         // Tüm rotanın polyline'ı (encoded polyline formatında)
    
    @SerializedName("errorMessage")
    val errorMessage: String = ""      // Hata durumunda mesaj (opsiyonel)
)

data class TransferInfo(
    @SerializedName("has_transfer")
    val hasTransfer: Boolean = false,   // Aktarma olup olmadığı
    
    @SerializedName("transfer_count")
    val transferCount: Int = 0,        // Aktarma sayısı
    
    @SerializedName("transfer_points")
    val transferPoints: List<TransferPoint> = emptyList() // Aktarma noktaları
)

data class TransferPoint(
    @SerializedName("location")
    val location: String = "",         // Aktarma yeri (örn: "Opera")
    
    @SerializedName("from_bus")
    val fromBus: String = "",          // Binilen otobüs/araç (örn: "203")
    
    @SerializedName("to_bus")
    val toBus: String = "",            // İnilen otobüs/araç (örn: "304")
    
    @SerializedName("arrival_time")
    val arrivalTime: String = "",      // Varış zamanı (örn: "22:57")
    
    @SerializedName("departure_time")
    val departureTime: String = "",    // Kalkış zamanı (örn: "23:06")
    
    @SerializedName("waiting_time")
    val waitingTime: String = ""       // Bekleme süresi (örn: "9 dakika")
)

data class Summary(
    @SerializedName("total_distance")
    val totalDistance: String = "",
    
    @SerializedName("total_duration")
    val totalDuration: String = "",
    
    @SerializedName("start_address")
    val startAddress: String = "",
    
    @SerializedName("end_address")
    val endAddress: String = "",
    
    @SerializedName("fare") 
    val fare: String = ""
)

data class RouteStep(
    @SerializedName("travel_mode")
    val type: String = "",             // Adım tipi (WALKING, TRANSIT, vb.)
    
    @SerializedName("walking_details")
    val walkingDetails: WalkingDetails? = null,
    
    @SerializedName("transit_details")
    val transitDetails: TransitDetails? = null, // Toplu taşıma detayları (opsiyonel)
    
    val startLocation: LatLng = LatLng(0.0, 0.0),    // Başlangıç noktası
    val endLocation: LatLng = LatLng(0.0, 0.0),      // Bitiş noktası
    
    @SerializedName("polyline")
    val polyline: String = "",         // Rota geometrisi (encoded polyline formatında)
    
    val distance: String = "",         // Adım mesafesi
    val duration: String = "",         // Adım süresi
    
    @SerializedName("instructions")
    val instruction: String = "",      // Adımın açıklaması
    
    @SerializedName("maneuver")
    val maneuver: String = ""          // Manevra tipi
)

data class WalkingDetails(
    @SerializedName("start_location")
    val startLocation: LatLng = LatLng(0.0, 0.0),
    
    @SerializedName("end_location")
    val endLocation: LatLng = LatLng(0.0, 0.0)
)

data class LatLng(
    val lat: Double,
    val lng: Double
)

data class TransitDetails(
    val line: TransitLine = TransitLine(),        // Hat bilgisi
    
    @SerializedName("departure_stop")
    val departureStop: Stop = Stop(),      // Kalkış durağı
    
    @SerializedName("arrival_stop")
    val arrivalStop: Stop = Stop(),        // Varış durağı
    
    @SerializedName("departure_time")
    val departureTime: String = "",    // Kalkış zamanı
    
    @SerializedName("arrival_time")
    val arrivalTime: String = "",       // Varış zamanı
    
    @SerializedName("num_stops")
    val numStops: Int = 0,             // Durak sayısı
    
    val headsign: String = ""          // Hat yön bilgisi
)

data class TransitLine(
    val name: String = "",             // Hat adı (örn: "Metrobüs", "M2")
    
    @SerializedName("short_name")
    val shortName: String = "",        // Kısa hat adı (örn: "34BZ")
    
    val color: String = "",            // Hat rengi (hex formatında)
    
    @SerializedName("text_color")
    val textColor: String = "",        // Metin rengi (hex formatında)
    
    @SerializedName("vehicle_type")
    val vehicle: String = ""           // Araç tipi (BUS, SUBWAY, TRAM vb.)
)

data class Stop(
    val name: String = "",             // Durak adı
    val location: LatLng = LatLng(0.0, 0.0)          // Durak konumu
) 