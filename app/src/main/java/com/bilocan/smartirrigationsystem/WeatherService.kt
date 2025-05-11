package com.bilocan.smartirrigationsystem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Hava durumu bilgisi modeli
data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

// OpenWeatherMap API arayüzü ekle
interface OpenWeatherMapService {
    @GET("weather")
    fun getCurrentWeather(
        @Query("q") cityName: String = "Ankara",
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "tr"
    ): Call<WeatherResponse>
    
    @GET("weather")
    fun getCurrentWeatherByCoord(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "tr"
    ): Call<WeatherResponse>
}

/**
 * OpenWeatherMap API 3.0 ile hava durumu verilerini almak için servis
 */
class WeatherService(private val context: Context) {
    
    private val TAG = "WeatherService"
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null
    
    // OpenWeatherMap One Call API 3.0 için Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/3.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // API servisi
    private val oneCallService = retrofit.create(OneCallService::class.java)
    
    // Normal Weather API için Retrofit
    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Weather API servisi
    private val weatherService = weatherRetrofit.create(OpenWeatherMapService::class.java)
    
    /**
     * Kullanıcının konumunu almak için izinleri kontrol eder.
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Kullanıcının son bilinen konumunu alır.
     */
    fun getLastLocation(callback: (Location?) -> Unit) {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        currentLocation = location
                        callback(location)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Konum alınamadı: ${e.message}")
                        callback(null)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "Konum izni alınamadı: ${e.message}")
                callback(null)
            }
        } else {
            callback(null)
        }
    }
    
    /**
     * Konum güncellemelerini başlatır.
     */
    fun startLocationUpdates(callback: (Location) -> Unit) {
        if (!hasLocationPermission()) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    currentLocation = it
                    callback(it)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Konum izni alınamadı: ${e.message}")
        }
    }
    
    /**
     * Konum güncellemelerini durdurur.
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
    
    /**
     * Belirtilen şehir için hava durumu verisi döndürür.
     * One Call API 3.0 yerine Weather API kullanır, çünkü One Call API şehir adıyla sorgu yapmayı desteklemez
     */
    fun getWeatherForCity(cityName: String, callback: (WeatherInfo?) -> Unit) {
        try {
            val apiKey = BuildConfig.WEATHER_API_KEY
            Log.d(TAG, "Kullanılan API Key: $apiKey")
            
            // Önce şehir adı ile koordinat bilgilerini al
            weatherService.getCurrentWeather(cityName = cityName, apiKey = apiKey, lang = "tr")
                .enqueue(object : Callback<WeatherResponse> {
                    override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                        if (response.isSuccessful) {
                            val weatherResponse = response.body()
                            Log.d(TAG, "API yanıtı: $weatherResponse")
                            
                            weatherResponse?.let {
                                // Koordinatları alıp One Call API için kullan
                                getWeatherByCoordinates(it.coord.lat, it.coord.lon) { weatherInfo ->
                                    if (weatherInfo != null) {
                                        // Şehir adını ekle (One Call API şehir adı dönmüyor)
                                        val updatedInfo = weatherInfo.copy(city = it.name, country = it.sys.country)
                                        callback(updatedInfo)
                                    } else {
                                        // One Call API başarısız olursa normal Weather API'den veri oluştur
                                        val weatherInfo = WeatherInfo(
                                            city = it.name,
                                            country = it.sys.country,
                                            description = if (it.weather.isNotEmpty()) it.weather[0].description else "",
                                            temperature = it.main.temp,
                                            feelsLike = it.main.feels_like,
                                            minTemp = it.main.temp_min,
                                            maxTemp = it.main.temp_max,
                                            humidity = it.main.humidity,
                                            pressure = it.main.pressure,
                                            windSpeed = it.wind.speed,
                                            iconCode = if (it.weather.isNotEmpty()) it.weather[0].icon else "01d",
                                            sunrise = it.sys.sunrise,
                                            sunset = it.sys.sunset
                                        )
                                        callback(weatherInfo)
                                    }
                                }
                            } ?: callback(null)
                        } else {
                            Log.e(TAG, "API Hatası: ${response.code()} - ${response.message()}")
                            callback(null)
                        }
                    }
                    
                    override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                        Log.e(TAG, "API Bağlantı Hatası", t)
                        callback(null)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Hava durumu verisi alınırken hata oluştu", e)
            callback(null)
        }
    }
    
    /**
     * One Call API 3.0 kullanarak koordinatlara göre hava durumu verisi getirir
     */
    private fun getWeatherByCoordinates(lat: Double, lon: Double, callback: (WeatherInfo?) -> Unit) {
        try {
            val apiKey = BuildConfig.WEATHER_API_KEY
            
            oneCallService.getOneCallWeather(
                lat = lat,
                lon = lon,
                apiKey = apiKey,
                units = "metric",
                lang = "tr"
            ).enqueue(object : Callback<OneCallResponse> {
                override fun onResponse(call: Call<OneCallResponse>, response: Response<OneCallResponse>) {
                    if (response.isSuccessful) {
                        val oneCallResponse = response.body()
                        Log.d(TAG, "One Call API yanıtı: $oneCallResponse")
                        
                        oneCallResponse?.let {
                            val current = it.current
                            val weatherInfo = WeatherInfo(
                                city = "", // One Call API şehir adı dönmüyor, Weather API'den alınacak
                                country = "", // One Call API ülke adı dönmüyor, Weather API'den alınacak
                                description = if (current.weather.isNotEmpty()) current.weather[0].description else "",
                                temperature = current.temp,
                                feelsLike = current.feels_like,
                                minTemp = current.temp, // One Call API'da güncel sıcaklık için min/max yok
                                maxTemp = current.temp, // One Call API'da güncel sıcaklık için min/max yok
                                humidity = current.humidity,
                                pressure = current.pressure,
                                windSpeed = current.wind_speed,
                                iconCode = if (current.weather.isNotEmpty()) current.weather[0].icon else "01d",
                                sunrise = current.sunrise,
                                sunset = current.sunset
                            )
                            callback(weatherInfo)
                        } ?: callback(null)
                    } else {
                        Log.e(TAG, "One Call API Hatası: ${response.code()} - ${response.message()}")
                        callback(null)
                    }
                }
                
                override fun onFailure(call: Call<OneCallResponse>, t: Throwable) {
                    Log.e(TAG, "One Call API Bağlantı Hatası", t)
                    callback(null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "One Call API ile hava durumu verisi alınırken hata oluştu", e)
            callback(null)
        }
    }
    
    /**
     * Kullanıcının mevcut konumu için hava durumu verisi döndürür.
     */
    fun getWeatherForCurrentLocation(callback: (WeatherInfo?) -> Unit) {
        getLastLocation { location ->
            if (location != null) {
                getWeatherByCoordinates(location.latitude, location.longitude, callback)
            } else {
                // Konum alınamadığında Ankara, Çankaya için hava durumu verisi al
                getWeatherForCity("Çankaya,Ankara,tr", callback)
            }
        }
    }
}

/**
 * One Call API 3.0 arayüzü
 */
interface OneCallService {
    @GET("onecall")
    fun getOneCallWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "tr",
        @Query("exclude") exclude: String = "minutely,alerts"
    ): Call<OneCallResponse>
}

/**
 * One Call API 3.0 yanıt modeli
 */
data class OneCallResponse(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val timezone_offset: Int,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>? = null,
    val daily: List<DailyWeather>? = null
)

data class CurrentWeather(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val temp: Double,
    val feels_like: Double,
    val pressure: Int,
    val humidity: Int,
    val dew_point: Double,
    val uvi: Double,
    val clouds: Int,
    val visibility: Int,
    val wind_speed: Double,
    val wind_deg: Int,
    val wind_gust: Double? = null,
    val weather: List<Weather>
)

data class HourlyWeather(
    val dt: Long,
    val temp: Double,
    val feels_like: Double,
    val pressure: Int,
    val humidity: Int,
    val dew_point: Double,
    val uvi: Double,
    val clouds: Int,
    val visibility: Int,
    val wind_speed: Double,
    val wind_deg: Int,
    val wind_gust: Double? = null,
    val weather: List<Weather>,
    val pop: Double
)

data class DailyWeather(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val moonrise: Long,
    val moonset: Long,
    val moon_phase: Double,
    val temp: Temperature,
    val feels_like: FeelsLike,
    val pressure: Int,
    val humidity: Int,
    val dew_point: Double,
    val wind_speed: Double,
    val wind_deg: Int,
    val wind_gust: Double? = null,
    val weather: List<Weather>,
    val clouds: Int,
    val pop: Double,
    val uvi: Double,
    val rain: Double? = null
)

data class Temperature(
    val day: Double,
    val min: Double,
    val max: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

data class FeelsLike(
    val day: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

/**
 * Hava durumu bilgilerini içeren veri sınıfı
 */
data class WeatherInfo(
    val city: String,
    val country: String,
    val description: String,
    val temperature: Double,
    val feelsLike: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val iconCode: String,
    val sunrise: Long,
    val sunset: Long
) {
    /**
     * Gün doğumu saatini biçimlendirilmiş string olarak döndürür
     */
    fun getSunriseFormatted(): String {
        val date = Date(sunrise * 1000)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Gün batımı saatini biçimlendirilmiş string olarak döndürür
     */
    fun getSunsetFormatted(): String {
        val date = Date(sunset * 1000)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Standart hava durumu ikonunun URL'sini döndürür
     */
    fun getIconUrl(): String {
        return "https://openweathermap.org/img/w/$iconCode.png"
    }
    
    /**
     * Yüksek çözünürlüklü hava durumu ikonunun URL'sini döndürür
     */
    fun getHighResIconUrl(): String {
        return "https://openweathermap.org/img/wn/$iconCode@4x.png"
    }
}

// WeatherResponse modeli sınıf sonuna eklendi
data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)

data class Wind(
    val speed: Double,
    val deg: Int
)

data class Clouds(
    val all: Int
)

data class Sys(
    val type: Int,
    val id: Int,
    val country: String,
    val sunrise: Long,
    val sunset: Long
) 