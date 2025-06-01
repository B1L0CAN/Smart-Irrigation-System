# Smart Irrigation System 🌱

Smart Irrigation System is an ESP32-based IoT project. This system collects data through sensors such as soil moisture, rain status, and air temperature, and performs automatic irrigation based on this data.

## 🚀 Features

- 🌡️ Temperature and humidity sensor support
- 💧 Soil moisture sensor
- 🌧️ Rain sensor
- 📱 Remote control with Android application
- 📊 Real-time data monitoring
- 📈 Weather integration
- 🔔 Smart irrigation alerts
- 📝 Detailed irrigation logs

## 🛠️ Technologies

- **Hardware:**
  - ESP32
  - DHT22 Temperature/Humidity Sensor
  - Soil Moisture Sensor
  - Rain Sensor
  - Water Flow Sensor
  - Relay Module

- **Software:**
  - Android Studio
  - Kotlin
  - Firebase Realtime Database
  - WeatherAPI.com

## 📋 Installation

### Required API Keys

1. **WeatherAPI.com**
   - Create a free account at [WeatherAPI.com](https://www.weatherapi.com/)
   - Get your API key
   - Replace `YOUR_WEATHER_API_KEY` in `MainActivity.kt` with your API key

2. **Firebase**
   - Create a new project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Realtime Database
   - Add `google-services.json` to your project
   - Update the following values in `IrrigationApp.kt`:
     ```kotlin
     private const val DB_URL = "YOUR_FIREBASE_DATABASE_URL"
     private const val API_KEY = "YOUR_FIREBASE_API_KEY"
     private const val APP_ID = "YOUR_FIREBASE_APP_ID"
     ```

### ESP32 Setup

1. Load ESP32 code into Arduino IDE
2. Install required libraries:
   - Firebase ESP Client
   - DHT sensor library
3. Update WiFi and Firebase information in `esp_code.txt`:
   ```cpp
   #define WIFI_SSID "YOUR_WIFI_SSID"
   #define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
   #define API_KEY "YOUR_FIREBASE_API_KEY"
   #define DATABASE_URL "YOUR_FIREBASE_DATABASE_URL"
   #define USER_EMAIL "YOUR_FIREBASE_USER_EMAIL"
   #define USER_PASSWORD "YOUR_FIREBASE_USER_PASSWORD"
   ```

### Android Application

1. Open the project in Android Studio
2. Install required dependencies
3. Update API keys
4. Build and run the application

## 📱 Application Features

- Real-time sensor data monitoring
- Manual and automatic irrigation control
- Weather forecasts
- Detailed irrigation logs
- System status monitoring

## 🎥 Demo Video

You can watch the [YouTube video](https://www.youtube.com/watch?v=HgPWbnkD3RU&t=1s&ab_channel=B1L0) for a detailed demonstration and usage of the project.

## 📞 Contact

Musa Bilal YAZ - [LinkedIn](https://www.linkedin.com/in/musa-bilal-yaz-515226232/)

Project Link: [https://github.com/B1L0CAN/Smart-Irrigation-System](https://github.com/B1L0CAN/Smart-Irrigation-System) 