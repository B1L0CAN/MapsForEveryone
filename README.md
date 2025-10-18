# Maps For Everyone

Görme engelli kullanıcılar için özel olarak tasarlanmış, erişilebilir Android harita uygulaması.

## 🎯 Proje Hakkında

Maps For Everyone, görme engelli kullanıcıların bağımsız olarak navigasyon yapabilmelerini sağlamak amacıyla geliştirilmiştir. Uygulama, sesli komutlar, yüksek kontrast modu ve erişilebilirlik özellikleri ile kullanıcı dostu bir deneyim sunar.

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
├── app/                          # Ana Android uygulaması
│   ├── src/main/java/com/bilocan/mapsforeveryone/
│   │   ├── api/                  # API servisleri ve repository'ler
│   │   ├── data/                 # Veri modelleri
│   │   ├── ui/                   # Kullanıcı arayüzü
│   │   │   ├── home/            # Ana sayfa - Harita ve navigasyon
│   │   │   ├── favorites/       # Favoriler - Konum yönetimi
│   │   │   ├── login/           # Giriş - Kullanıcı kimlik doğrulama
│   │   │   ├── register/        # Kayıt - Yeni kullanıcı oluşturma
│   │   │   └── settings/        # Ayarlar - Erişilebilirlik seçenekleri
│   │   ├── service/             # Servis sınıfları
│   │   ├── MainActivity.kt      # Ana Activity
│   │   └── MapsApplication.kt   # Application sınıfı
│   └── src/main/res/            # Kaynaklar (layout, drawable, vb.)
├── mapson/                      # Alternatif Android modülü
├── build.gradle               # Proje build konfigürasyonu
├── settings.gradle            # Gradle ayarları
├── local.properties.example  # Yerel özellikler örneği
└── README.md                  # Proje dokümantasyonu
```

## 🛠️ Teknolojiler

### Android Uygulaması
- **Android (Kotlin)** - Native mobil uygulama
- **Google Maps SDK** - Harita entegrasyonu
- **Retrofit** - HTTP istekleri
- **Text-to-Speech** - Sesli geri bildirim
- **Speech Recognition** - Sesli komutlar
- **Navigation Component** - Sayfa geçişleri

## 📱 Kurulum

### Gereksinimler
- **Android Studio** Arctic Fox veya üzeri
- **Android SDK** 24+
- **Google Maps API Key**

### Kurulum Adımları

1. **Projeyi klonlayın:**
```bash
git clone https://github.com/B1L0CAN/MapsForEveryone.git
cd MapsForEveryone
```

2. **API anahtarlarını yapılandırın:**
```bash
# local.properties dosyası oluşturun
GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
BACKEND_URL=http://your-backend-url:8080/
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

### Android Yapılandırması
- `local.properties` dosyasında backend URL'ini güncelleyin
- Google Maps API anahtarını ekleyin
- Gerekli izinleri `AndroidManifest.xml` dosyasında kontrol edin

## 📞 İletişim

- **GitHub:** [@B1L0CAN](https://github.com/B1L0CAN)
- **Proje Linki:** [MapsForEveryone](https://github.com/B1L0CAN/MapsForEveryone)

## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

---

**Not:** Bu uygulama görme engelli kullanıcılar için özel olarak tasarlanmış olsa da erişilebilirlik özellikleri tüm kullanıcılar için faydalıdır.