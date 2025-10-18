# Maps For Everyone

Görme engelli kullanıcılar için özel olarak tasarlanmış, erişilebilir harita uygulaması. Backend (Spring Boot) ve Android uygulamasından oluşan tam stack proje.

## 🎯 Proje Hakkında

Maps For Everyone, görme engelli kullanıcıların bağımsız olarak navigasyon yapabilmelerini sağlamak amacıyla geliştirilmiştir. Proje, backend API servisleri ve Android mobil uygulamasından oluşan tam bir ekosistemdir.

## ✨ Ana Özellikler

### 🗺️ Harita ve Navigasyon
- **Google Maps entegrasyonu** - Gerçek zamanlı harita görüntüleme
- **Toplu taşıma rotaları** - Otobüs, metro, tramvay rotaları
- **Sesli navigasyon** - Adım adım sesli yol tarifi
- **Favori konumlar** - Sık kullanılan adresleri kaydetme

### 🎤 Sesli Kontrol
- **Sesli komutlar** - Mikrofon ile adres arama
- **Text-to-Speech** - Türkçe sesli geri bildirim
- **Çift tıklama sistemi** - Yanlışlıkla tıklamaları önleme

### ♿ Erişilebilirlik
- **Yüksek kontrast modu** - Görme zorluğu olan kullanıcılar için
- **Büyük yazı boyutu** - Okunabilirlik için ayarlanabilir font
- **Sesli bildirimler** - Her işlem için sesli geri bildirim
- **Basit arayüz** - Karmaşık menüler yerine sade tasarım

## 🏗️ Proje Yapısı

```
MapsForEveryone/
├── app/                          # Android uygulaması
│   ├── src/main/java/com/bilocan/mapsforeveryone/
│   │   ├── api/                  # API servisleri ve repository'ler
│   │   │   ├── model/            # API request/response modelleri
│   │   │   ├── ApiService.kt     # Retrofit API servisi
│   │   │   ├── RetrofitClient.kt # HTTP istemci yapılandırması
│   │   │   ├── AuthRepository.kt # Kimlik doğrulama repository'si
│   │   │   ├── LocationRepository.kt # Konum repository'si
│   │   │   └── TransitRepository.kt # Toplu taşıma repository'si
│   │   ├── data/                 # Veri modelleri
│   │   │   ├── FavoriteLocation.kt # Favori konum modeli
│   │   │   ├── FavoriteRepository.kt # Favori repository interface'i
│   │   │   └── SharedPreferencesFavoriteRepository.kt
│   │   ├── ui/                   # Kullanıcı arayüzü
│   │   │   ├── home/            # Ana sayfa - Harita ve navigasyon
│   │   │   ├── favorites/       # Favoriler - Konum yönetimi
│   │   │   ├── login/           # Giriş - Kullanıcı kimlik doğrulama
│   │   │   ├── register/        # Kayıt - Yeni kullanıcı oluşturma
│   │   │   └── settings/        # Ayarlar - Erişilebilirlik seçenekleri
│   │   ├── service/             # Servis sınıfları
│   │   │   ├── TransitService.java # Toplu taşıma servisi interface'i
│   │   │   └── TransitServiceImpl.java # Toplu taşıma servisi implementasyonu
│   │   ├── MainActivity.kt      # Ana Activity
│   │   ├── MapsApplication.kt   # Application sınıfı  
│   ├── src/main/res/            # Kaynaklar (layout, drawable, vb.)
│   ├── build.gradle            # Uygulama build konfigürasyonu
│   └── proguard-rules.pro      # ProGuard kuralları
├── demo/                        # Backend (Spring Boot) - [Arslanmcahid'in repo'sundan](https://github.com/arslanmcahid/MapsForEveryone)
│   ├── src/main/java/          # Spring Boot backend kodu
│   ├── src/main/resources/     # Backend konfigürasyon dosyaları
│   ├── pom.xml                 # Maven konfigürasyonu
│   └── application.properties  # Backend ayarları
├── build.gradle               # Proje seviyesi build konfigürasyonu
├── settings.gradle            # Gradle ayarları
├── gradle.properties          # Gradle özellikleri
├── local.properties.example  # Yerel özellikler örneği
└── README.md                  # Proje dokümantasyonu
```

## 🛠️ Teknolojiler

### Backend (Spring Boot)
- **Spring Boot** - RESTful API geliştirme
- **JWT Authentication** - Güvenli kimlik doğrulama
- **MSSQL** - Veritabanı yönetimi
- **Google Maps API** - Harita ve rota servisleri
- **Text-to-Speech API** - Sesli geri bildirim
- **Server-Sent Events (SSE)** - Gerçek zamanlı iletişim

### Android Uygulaması
- **Android (Kotlin)** - Native mobil uygulama
- **Google Maps SDK** - Harita entegrasyonu
- **Retrofit** - HTTP istekleri
- **Text-to-Speech** - Sesli geri bildirim
- **Speech Recognition** - Sesli komutlar
- **Navigation Component** - Sayfa geçişleri

## 📱 Kurulum

### Gereksinimler
- **Backend için:** Java 8+, Maven, MSSQL
- **Android için:** Android Studio Arctic Fox+, Android SDK 24+
- **API Anahtarları:** Google Maps, Google TTS

### Kurulum Dosyaları 

- **GitHub:** [@B1L0CAN](https://github.com/B1L0CAN)
- **Proje Linki:** [MapsForEveryone](https://github.com/B1L0CAN/MapsForEveryone)
- **Backend Repository:** [Arslanmcahid/MapsForEveryone](https://github.com/arslanmcahid/MapsForEveryone)

### Backend Kurulumu

1. **Backend repository'sini klonlayın:**
```bash
git clone https://github.com/arslanmcahid/MapsForEveryone.git
cd MapsForEveryone/demo
```

2. **Veritabanı ve API anahtarlarını yapılandırın:**
```bash
# application.properties dosyasını düzenleyin
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

3. **Backend'i başlatın:**
```bash
mvn clean install
mvn spring-boot:run
```

### Android Kurulumu

1. **Android projesini klonlayın:**
```bash
git clone https://github.com/B1L0CAN/MapsForEveryone.git
cd MapsForEveryone
```

2. **API anahtarlarını yapılandırın:**
```bash
# local.properties dosyası oluşturun
GOOGLE_MAPS_API_KEY=your_api_key_here
BACKEND_URL=http://localhost:8080/
```

3. **Android Studio'da projeyi açın ve çalıştırın**

## 🎮 Kullanım

### Sesli Komutlar
- **Mikrofon butonuna çift tıklayın** - Sesli komut modunu başlatır
- **"Ankara Kızılay'a git"** gibi komutlar söyleyin
- Uygulama komutu anlayıp rota hesaplayacaktır

### Favori Konumlar
1. Favoriler sekmesine gidin
2. "Favori Ekle" butonuna çift tıklayın
3. Konum adı ve adres bilgilerini girin
4. Kaydedilen konumlara tek tıklayarak navigasyon başlatabilirsiniz

### Erişilebilirlik Ayarları
- Ayarlar sekmesinden kontrast modunu açabilirsiniz
- Yazı boyutunu büyütebilirsiniz
- Sesli bildirimleri özelleştirebilirsiniz

## 🔧 Yapılandırma

### Backend Yapılandırması
- `application.properties` dosyasında veritabanı bağlantı bilgilerini güncelleyin
- JWT secret key'i güvenli bir değerle değiştirin
- Google Maps ve TTS API anahtarlarını ekleyin

### Android Yapılandırması
- `local.properties` dosyasında backend URL'ini güncelleyin
- Google Maps API anahtarını ekleyin
- Gerekli izinleri `AndroidManifest.xml` dosyasında kontrol edin


## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

---

**Not:** Bu uygulama görme engelli kullanıcılar için özel olarak tasarlanmış olsa da erişilebilirlik özellikleri tüm kullanıcılar için faydalıdır.