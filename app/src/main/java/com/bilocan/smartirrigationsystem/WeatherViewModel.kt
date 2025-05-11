package com.bilocan.smartirrigationsystem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Hava durumu için ViewModel
 */
class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = WeatherRepository(application)
    
    // Hava durumu verileri
    val weatherData: LiveData<WeatherInfo> = repository.weatherData
    
    // Hata durumu
    val error: LiveData<String> = repository.error
    
    // Yükleniyor durumu
    val isLoading: LiveData<Boolean> = repository.isLoading
    
    // Güncelleme durumu (true = güncelleme başarılı)
    private val _weatherUpdateSuccess = MutableLiveData<Boolean>()
    val weatherUpdateSuccess: LiveData<Boolean> = _weatherUpdateSuccess
    
    // Başlangıçta güncelleme durumunu false olarak ayarla
    init {
        _weatherUpdateSuccess.value = false
        
        // ViewModel başlatıldığında hava durumu verilerini getir
        fetchWeather("Ankara,tr")
    }
    
    /**
     * Hava durumu verilerini getir
     */
    fun fetchWeather(cityName: String = "Ankara,tr", useWeatherService: Boolean = false) {
        // Güncelleme başlatıldığında değeri sıfırla
        _weatherUpdateSuccess.value = false
        
        repository.fetchWeather(cityName, useWeatherService)
    }
    
    /**
     * Mevcut konum için hava durumu verilerini getir
     */
    fun fetchWeatherForCurrentLocation() {
        // Güncelleme başlatıldığında değeri sıfırla
        _weatherUpdateSuccess.value = false
        
        repository.fetchWeatherForCurrentLocation()
    }
    
    /**
     * Repository'deki değişiklikleri kontrol et ve güncelleme başarılı değerini ayarla
     * Bu method repository değerleri alındığında çağrılmalıdır
     */
    fun checkAndNotifyWeatherUpdate() {
        // Yükleme tamamlandığında ve veri varsa başarılı olarak işaretle
        if (weatherData.value != null && isLoading.value == false) {
            _weatherUpdateSuccess.value = true
        }
    }
} 