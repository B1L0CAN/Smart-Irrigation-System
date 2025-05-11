package com.bilocan.smartirrigationsystem

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Hava durumu verilerine erişimi yöneten repository sınıfı
 */
class WeatherRepository(private val context: Context) {
    
    private val TAG = "WeatherRepository"
    
    // OpenWeatherMap API için Retrofit ayarları
    private val openWeatherMapRetrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val openWeatherMapService = openWeatherMapRetrofit.create(OpenWeatherMapService::class.java)
    
    // Hava durumu servisi
    private val weatherService = WeatherService(context)
    
    // Hava durumu verilerini tutan LiveData
    private val _weatherData = MutableLiveData<WeatherInfo>()
    val weatherData: LiveData<WeatherInfo> = _weatherData
    
    // Hata durumunda
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // Yükleniyor durumu
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * OpenWeatherMap API'den şehir adına göre hava durumu verilerini getirir
     */
    fun fetchWeatherFromOpenWeatherMap(cityName: String) {
        _isLoading.value = true
        
        try {
            val apiKey = BuildConfig.WEATHER_API_KEY
            openWeatherMapService.getCurrentWeather(cityName = cityName, apiKey = apiKey).enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    _isLoading.value = false
                    
                    if (response.isSuccessful) {
                        val weatherResponse = response.body()
                        weatherResponse?.let {
                            // OpenWeatherMap yanıtını WeatherInfo modelimize dönüştür
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
                            _weatherData.value = weatherInfo
                        }
                    } else {
                        // API hatası, weatherService'i kullanarak yeniden dene
                        fetchWeatherFromService(cityName)
                    }
                }
                
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = "Hava durumu verileri alınamadı: ${t.message}"
                    Log.e(TAG, "API Hatası", t)
                    
                    // Hata durumunda weatherService'i kullanarak yeniden dene
                    fetchWeatherFromService(cityName)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "API isteği yapılırken hata oluştu", e)
            fetchWeatherFromService(cityName)
        }
    }
    
    /**
     * WeatherService ile hava durumu verilerini getirir
     */
    fun fetchWeatherFromService(cityName: String) {
        _isLoading.value = true
        
        weatherService.getWeatherForCity(cityName) { weatherInfo ->
            _isLoading.value = false
            
            if (weatherInfo != null) {
                _weatherData.value = weatherInfo
            } else {
                _error.value = "Hava durumu verileri alınamadı"
                Log.e(TAG, "Hava durumu servisi verisi alınamadı")
            }
        }
    }
    
    /**
     * Mevcut konum için hava durumu verilerini getirir
     */
    fun fetchWeatherForCurrentLocation() {
        _isLoading.value = true
        
        if (weatherService.hasLocationPermission()) {
            weatherService.getWeatherForCurrentLocation { weatherInfo ->
                _isLoading.value = false
                
                if (weatherInfo != null) {
                    _weatherData.value = weatherInfo
                } else {
                    _error.value = "Hava durumu verileri alınamadı"
                    Log.e(TAG, "Konum için hava durumu verileri alınamadı")
                }
            }
        } else {
            _isLoading.value = false
            _error.value = "Konum izinleri verilmedi"
            Log.e(TAG, "Konum izinleri verilmedi")
        }
    }
    
    /**
     * API tipine göre hava durumu verilerini getirir
     */
    fun fetchWeather(cityName: String, useWeatherService: Boolean = false) {
        if (useWeatherService) {
            fetchWeatherFromService(cityName)
        } else {
            fetchWeatherFromOpenWeatherMap(cityName)
        }
    }
} 