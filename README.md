# Smart Irrigation System 🌱

Akıllı sulama sistemi, ESP32 tabanlı bir IoT projesidir. Bu sistem, sensörler aracılığıyla toprak nemi, yağmur durumu ve hava sıcaklığı gibi verileri toplar ve bu verilere göre otomatik sulama yapar.

## 🚀 Özellikler

- 🌡️ Sıcaklık ve nem sensörü desteği
- 💧 Toprak nem sensörü
- 🌧️ Yağmur sensörü
- 📱 Android uygulaması ile uzaktan kontrol
- 📊 Gerçek zamanlı veri izleme
- 📈 Hava durumu entegrasyonu
- 🔔 Akıllı sulama uyarıları
- 📝 Detaylı sulama logları

## 🛠️ Teknolojiler

- **Donanım:**
  - ESP32
  - DHT22 Sıcaklık/Nem Sensörü
  - Toprak Nem Sensörü
  - Yağmur Sensörü
  - Su Akış Sensörü
  - Röle Modülü

- **Yazılım:**
  - Android Studio
  - Kotlin
  - Firebase Realtime Database
  - WeatherAPI.com

## 📋 Kurulum

### Gerekli API Anahtarları

1. **WeatherAPI.com**
   - [WeatherAPI.com](https://www.weatherapi.com/) adresinden ücretsiz hesap oluşturun
   - API anahtarınızı alın
   - `MainActivity.kt` dosyasında `YOUR_WEATHER_API_KEY` yerine API anahtarınızı ekleyin

2. **Firebase**
   - [Firebase Console](https://console.firebase.google.com/) adresinden yeni proje oluşturun
   - Realtime Database'i etkinleştirin
   - `google-services.json` dosyasını projenize ekleyin
   - `IrrigationApp.kt` dosyasında aşağıdaki değerleri güncelleyin:
     ```kotlin
     private const val DB_URL = "YOUR_FIREBASE_DATABASE_URL"
     private const val API_KEY = "YOUR_FIREBASE_API_KEY"
     private const val APP_ID = "YOUR_FIREBASE_APP_ID"
     ```

### ESP32 Kurulumu

1. ESP32 kodunu Arduino IDE'ye yükleyin
2. Gerekli kütüphaneleri yükleyin:
   - Firebase ESP Client
   - DHT sensor library
3. `esp_code.txt` dosyasındaki WiFi ve Firebase bilgilerini güncelleyin:
   ```cpp
   #define WIFI_SSID "YOUR_WIFI_SSID"
   #define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
   #define API_KEY "YOUR_FIREBASE_API_KEY"
   #define DATABASE_URL "YOUR_FIREBASE_DATABASE_URL"
   #define USER_EMAIL "YOUR_FIREBASE_USER_EMAIL"
   #define USER_PASSWORD "YOUR_FIREBASE_USER_PASSWORD"
   ```

### Android Uygulaması

1. Projeyi Android Studio'da açın
2. Gerekli bağımlılıkları yükleyin
3. API anahtarlarını güncelleyin
4. Uygulamayı derleyin ve çalıştırın

## 📱 Uygulama Özellikleri

- Gerçek zamanlı sensör verilerini görüntüleme
- Manuel ve otomatik sulama kontrolü
- Hava durumu tahminleri
- Detaylı sulama logları
- Sistem durumu izleme

## 🎥 Tanıtım Videosu

Projenin detaylı tanıtımı ve kullanımı için [YouTube videosunu](https://www.youtube.com/watch?v=HgPWbnkD3RU&t=1s&ab_channel=B1L0) izleyebilirsiniz.

## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## 👥 Katkıda Bulunma

1. Bu depoyu fork edin
2. Yeni bir özellik dalı oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add some amazing feature'`)
4. Dalınıza push edin (`git push origin feature/amazing-feature`)
5. Bir Pull Request oluşturun

## 📞 İletişim

B1L0CAN - [@B1L0CAN](https://github.com/B1L0CAN)

Proje Linki: [https://github.com/B1L0CAN/Smart-Irrigation-System](https://github.com/B1L0CAN/Smart-Irrigation-System) 