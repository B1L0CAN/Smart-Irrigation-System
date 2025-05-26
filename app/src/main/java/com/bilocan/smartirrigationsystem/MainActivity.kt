package com.bilocan.smartirrigationsystem

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.Timer
import java.util.TimerTask
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.TimeZone
import android.widget.ImageView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.view.ViewGroup
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bilocan.smartirrigationsystem.model.ForecastDay
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import android.view.ViewTreeObserver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    // Arayüz elemanları
    private lateinit var tvSicaklik: TextView
    private lateinit var tvNem: TextView
    private lateinit var tvSuMiktari: TextView
    private lateinit var tvVanaDurum: TextView
    private lateinit var tvVanaCalismaSuresi: TextView
    private lateinit var tvBosKutu: TextView
    private lateinit var btnVanaAc: Button
    private lateinit var btnVanaKapat: Button
    private lateinit var btnLoglar: Button
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvToprakNem: TextView
    private lateinit var tvYagmur: TextView
    private lateinit var rvForecast: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter
    private var forecastList: MutableList<ForecastDay> = mutableListOf()
    private lateinit var tvTodaySummary: TextView

    // Vana durumu için değişkenler
    private var isVanaAcik = false
    private var vanaSonAcilisSaati: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null
    
    // Sensör verileri
    private var sicaklik = 25.0f
    private var nem = 60.0f
    private var suMiktari = 0.0f
    private var bataryaDurumu = 85.0f
    private var sarjOluyor = true
    private var sistemDurumu = "Aktif"
    private var toprakNem = 70.0f
    private var yagmur = 0.0f
    private var bosAlan = "-"
    
    // Hava durumu ViewModel
    private lateinit var weatherViewModel: WeatherViewModel
    
    // Firebase Manager
    private lateinit var firebaseManager: FirebaseManager
    
    // ESP32 bağlantı durumu
    private var isESP32Connected = true
    
    // Sınıf değişkenlerine son veri alım zamanı ekleyelim
    private var lastDataReceived: Long = 0
    private var lastSensorSnapshot: String? = null
    private val DATA_TIMEOUT = 60000L // 1 dakika
    private var statusTimer: Timer? = null
    
    private var weatherRetryCount = 0
    private val MAX_RETRY_COUNT = 3
    private val RETRY_DELAY = 2000L // 2 saniye
    
    // Başlangıç sensör verileri için değişkenler
    private var logBaslangicSicaklik = 0.0f
    private var logBaslangicNem = 0.0f
    private var logBaslangicToprakNem = ""
    private var logBaslangicYagmur = ""
    private var logBaslangicSu = 0.0f
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Firebase'i başlat
        try {
            FirebaseApp.initializeApp(this)
            firebaseManager = FirebaseManager.getInstance()
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // ViewModel başlat
        weatherViewModel = ViewModelProvider(this).get(WeatherViewModel::class.java)
        
        // Arayüz elemanlarını bağlama
        tvSicaklik = findViewById(R.id.tvSicaklik)
        tvNem = findViewById(R.id.tvNem)
        tvSuMiktari = findViewById(R.id.tvSuMiktari)
        tvVanaDurum = findViewById(R.id.tvVanaDurum)
        tvVanaCalismaSuresi = findViewById(R.id.tvVanaCalismaSuresi)
        tvBosKutu = findViewById(R.id.tvBosKutu)
        btnVanaAc = findViewById(R.id.btnVanaAc)
        btnVanaKapat = findViewById(R.id.btnVanaKapat)
        btnLoglar = findViewById(R.id.btnLoglar)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvToprakNem = findViewById(R.id.tvToprakNem)
        tvYagmur = findViewById(R.id.tvYagmur)
        rvForecast = findViewById(R.id.rvForecast)
        forecastAdapter = ForecastAdapter(forecastList)
        rvForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvForecast.adapter = forecastAdapter
        tvTodaySummary = findViewById(R.id.tvTodaySummary)
        
        // Başlangıç durumunu ayarla
        updateVanaAcikKapaliDurumu()
        
        // Firebase'den verileri dinle
        listenToFirebaseData()
        
        // ESP32 bağlantı durumunu kontrol et
        checkESP32Connection()
        
        // Test sensör verileri gönder
        sendTestSensorData()
        
        // Sensör verilerini periyodik olarak güncelle (simüle edilmiş veriler)
        simuleSensorVerileri()
        
        // Hava durumu verilerini getir
        weatherViewModel.fetchWeather()
        
        // Buton click olayları
        btnVanaAc.setOnClickListener {
            val yesterdayRain = forecastList.getOrNull(1)?.desc?.contains("yağmur", true) == true
            val todayRain = forecastList.getOrNull(2)?.desc?.contains("yağmur", true) == true
            val tomorrowRain = forecastList.getOrNull(3)?.desc?.contains("yağmur", true) == true
            val soilWet = toprakNem > 60
            val rainSensorActive = tvYagmur.text.toString().contains("yağıyor", true)

            when {
                yesterdayRain -> showVanaAlert("Dün yağmur yağdı, vanayı açmak istediğinize emin misiniz?")
                todayRain -> showVanaAlert("Bugün hava yağmurlu, vanayı açmak istediğinize emin misiniz?")
                tomorrowRain -> showVanaAlert("Yarın hava yağmurlu gözüküyor, vanayı açmak istediğinize emin misiniz?")
                soilWet -> showVanaAlert("Toprağın nemi halihazırda %60'ın üzerinde, vanayı açmak istediğinize emin misiniz?")
                rainSensorActive -> showVanaAlert("Yağmur sensörü halihazırda yağmur algılıyor, vanayı açmak istediğinize emin misiniz?")
                else -> vanaAc()
            }
        }
        
        btnVanaKapat.setOnClickListener {
            vanaKapat()
        }
        
        btnLoglar.setOnClickListener {
            val intent = Intent(this, LogsActivity::class.java)
            startActivity(intent)
        }
        
        // Hava durumu gözlemcilerini ayarla
        setupWeatherObservers()
        
        // Düzenli olarak sistem durumunu kontrol et
        startSystemStatusChecker()
        
        fetchForecastWeather()
    }
    
    /**
     * Firebase'den sensör verilerini dinle
     */
    private fun listenToFirebaseData() {
        firebaseManager.listenToSensorData(
            onDataReceived = { temperature, humidity, isSoilWet, isRaining, flowRate, timestamp ->
                // Tüm verileri string olarak birleştir
                val currentSnapshot = "$temperature|$humidity|$isSoilWet|$isRaining|$flowRate"
                if (lastSensorSnapshot == null || lastSensorSnapshot != currentSnapshot) {
                    lastDataReceived = System.currentTimeMillis()
                    lastSensorSnapshot = currentSnapshot
                    isESP32Connected = true // Veri geldiğinde ESP32 bağlı
                    sistemDurumu = "Aktif"
                    tvBosKutu.text = sistemDurumu
                }
                sicaklik = temperature
                nem = humidity
                suMiktari = flowRate
                tvToprakNem.text = if (isSoilWet) "Islak" else "Kuru"
                tvYagmur.text = if (isRaining) "Yağıyor" else "Yağmıyor"
                tvSicaklik.text = String.format("%.1f°C", sicaklik)
                tvNem.text = String.format("%.1f%%", nem)
                tvSuMiktari.text = String.format("%.2fL/s", suMiktari)
            },
            onError = { error ->
                Toast.makeText(this, "Veri okuma hatası: $error", Toast.LENGTH_SHORT).show()
            }
        )
        
        // ESP32 bağlantı durumunu dinle
        firebaseManager.listenToESP32Status(
            onStatusChanged = { isActive: Boolean ->
                isESP32Connected = isActive
                sistemDurumu = if (isActive) "Aktif" else "Pasif"
                tvBosKutu.text = sistemDurumu
                
                if (!isActive) {
                    // ESP32 bağlantısı koptuğunda son veri alım zamanını güncelle
                    lastDataReceived = 0
                    lastSensorSnapshot = null
                }
            },
            onError = { error: String ->
                Toast.makeText(this, "ESP32 durum kontrolü hatası: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    /**
     * Sensör verilerine göre UI'ı güncelle
     */
    private fun updateUIWithSensorData() {
        tvSicaklik.text = String.format("%.1f°C", sicaklik)
        tvNem.text = String.format("%.1f%%", nem)
        tvSuMiktari.text = String.format("%.3fL/s", suMiktari)
        // Sadece boolean değerlerden gösterim
        tvToprakNem.text = if (toprakNem == 1f) "Islak" else "Kuru"
        tvYagmur.text = if (yagmur == 1f) "Yağıyor" else "Yağmıyor"
    }
    
    /**
     * Sistem durumunu güncelle
     */
    private fun updateSystemStatus() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastData = currentTime - lastDataReceived
        sistemDurumu = if (lastDataReceived == 0L || timeSinceLastData > DATA_TIMEOUT) "Pasif" else "Aktif"
        tvBosKutu.text = sistemDurumu
    }
    
    /**
     * ESP32 bağlantı durumunu kontrol et
     */
    private fun checkESP32Connection() {
        // ESP32 bağlantı kontrolünü aktif ediyoruz
        firebaseManager.checkESP32Connection(
            onConnected = { lastSeen ->
                isESP32Connected = true
                val lastSeenDate = Date(lastSeen)
                val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
                
                // Son görüldüğü zamanı güncelle
                lastDataReceived = lastSeen
                
                // Sistem durumunu güncelle
                updateSystemStatus()
                
                // ESP32 bağlantısı kurulduğunu logla
                println("ESP32 bağlantısı kuruldu! Son görülme: ${dateFormat.format(lastSeenDate)}")
            },
            onDisconnected = {
                isESP32Connected = false
                
                // Sistem pasif olarak işaretle
                sistemDurumu = "Pasif"
                tvBosKutu.text = sistemDurumu
                
                // Bağlantı koptuğunu logla ve kullanıcıya bildir
                println("ESP32 bağlantısı koptu! Sistem pasif.")
                Toast.makeText(this, "ESP32 bağlantısı kurulamadı! Sistem pasif.", Toast.LENGTH_LONG).show()
            }
        )
    }
    
    // Vana açma fonksiyonu
    private fun vanaAc() {
        if (!isVanaAcik) {
            Toast.makeText(this, "Vana açma komutu gönderiliyor...", Toast.LENGTH_SHORT).show()
            println("Vana açma işlemi başlatılıyor")
            
            if (isESP32Connected) {
                // ESP32 üzerinden vana kontrolü
                firebaseManager.setValveState(
                    isOpen = true,
                    onSuccess = {
                        println("Vana açma başarılı")
                        isVanaAcik = true
                        vanaSonAcilisSaati = System.currentTimeMillis()
                        updateVanaAcikKapaliDurumu()
                        Toast.makeText(this, "Vana başarıyla açıldı", Toast.LENGTH_SHORT).show()
                        logVanaAcildi()
                    },
                    onError = { error ->
                        println("Vana açma hatası: $error")
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Simülasyon modu
                println("Simülasyon modunda vana açılıyor")
                isVanaAcik = true
                vanaSonAcilisSaati = System.currentTimeMillis()
                
                // Açılış değerlerini içeren detaylı bilgi
                val acilisDetaylari = """
                    Manuel olarak açıldı
                    Sıcaklık: ${sicaklik.toInt()}°C
                    Nem: ${nem.toInt()}%
                    Su Miktarı: ${String.format("%.1fL", suMiktari)}
                    Şarj Durumu: ${if (sarjOluyor) "Oluyor" else "Olmuyor"}
                    Sistem: $sistemDurumu
                """.trimIndent()
                
                // LogManager ile vana açılışını logla
                LogManager.logValveOpen(
                    message = "Vana açıldı",
                    timestamp = vanaSonAcilisSaati,
                    details = acilisDetaylari
                )
                
                // UI güncelle
                updateVanaAcikKapaliDurumu()
                
                // Zamanlayıcıyı başlat
                baslatzamanlayici()
                
                // Kullanıcıya bilgi ver
                Toast.makeText(this, "Vana başarıyla açıldı", Toast.LENGTH_SHORT).show()
                logVanaAcildi()
            }
        } else {
            // Vana zaten açıksa kullanıcıya bilgi ver
            Toast.makeText(this, "Vana zaten açık durumda", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Vana kapatma fonksiyonu
    private fun vanaKapat() {
        if (isVanaAcik) {
            Toast.makeText(this, "Vana kapatma komutu gönderiliyor...", Toast.LENGTH_SHORT).show()
            println("Vana kapatma işlemi başlatılıyor")
            
            if (isESP32Connected) {
                // ESP32 üzerinden vana kontrolü
                firebaseManager.setValveState(
                    isOpen = false,
                    onSuccess = {
                        println("Vana kapatma başarılı")
                        val kapatmaZamani = System.currentTimeMillis()
                        val sulamaSuresi = kapatmaZamani - vanaSonAcilisSaati
                        
                        // Vana durumunu güncelle
                        isVanaAcik = false
                        
                        // Zamanlayıcıyı durdur ve UI'ı güncelle
                        timer?.cancel()
                        timer = null
                        tvVanaCalismaSuresi.text = "Vananın Çalışma Süresi: 00:00:00"
                        tvVanaDurum.text = "Vana Durumu: KAPALI"
                        
                        // Eğer en son bir vana kapatma işlemi yapıldıysa, yeni bir işlem başlat (ayraç ekle)
                        if (LogManager.isLastActionClose()) {
                            // Önce ayraç ekle
                            LogManager.addLogWithSeparator(vanaSonAcilisSaati)
                        }
                        
                        // Açılış değerlerini içeren detaylı bilgi
                        val kapanisDetaylari = """
                            Sulama süresi: ${LogManager.formatDuration(sulamaSuresi)}
                        """.trimIndent()
                        
                        // LogManager ile vana kapanışını logla
                        logVanaKapatildi(kapatmaZamani, sulamaSuresi)
                        
                        // UI güncelle
                        updateVanaAcikKapaliDurumu()
                        
                        // Logları kaydet
                        LogManager.saveLogsToStorage(this)
                        
                        // Kullanıcıya bilgi ver
                        val sureBilgisi = LogManager.formatDuration(sulamaSuresi)
                        Toast.makeText(this, "Vana kapatıldı. Sulama süresi: $sureBilgisi", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        println("Vana kapatma hatası: $error")
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Simülasyon modu
                println("Simülasyon modunda vana kapatılıyor")
                val kapatmaZamani = System.currentTimeMillis()
                val sulamaSuresi = kapatmaZamani - vanaSonAcilisSaati
                
                // Vana durumunu güncelle
                isVanaAcik = false
                
                // Zamanlayıcıyı durdur
                timer?.cancel()
                timer = null
                
                // Kapanış detayları
                val kapanisDetaylari = """
                    Sulama süresi: ${LogManager.formatDuration(sulamaSuresi)}
                """.trimIndent()
                
                // LogManager ile vana kapanışını logla
                LogManager.addLog(LogEntry(
                    message = "Vana kapatıldı",
                    timestamp = kapatmaZamani,
                    type = LogType.INFO,
                    details = kapanisDetaylari
                ))
                
                // UI güncelle
                updateVanaAcikKapaliDurumu()
                
                // Logları kaydet
                LogManager.saveLogsToStorage(this)
                
                // Kullanıcıya bilgi ver
                val sureBilgisi = LogManager.formatDuration(sulamaSuresi)
                Toast.makeText(this, "Vana kapatıldı. Sulama süresi: $sureBilgisi", Toast.LENGTH_SHORT).show()
                logVanaKapatildi(kapatmaZamani, sulamaSuresi)
            }
        } else {
            // Vana zaten kapalıysa kullanıcıya bilgi ver
            Toast.makeText(this, "Vana zaten kapalı durumda", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Vana durumunu güncelleme fonksiyonu
    private fun updateVanaAcikKapaliDurumu() {
        tvVanaDurum.text = if (isVanaAcik) "Vana Durumu: AÇIK" else "Vana Durumu: KAPALI"
        
        if (isVanaAcik) {
            baslatzamanlayici()
        } else {
            tvVanaCalismaSuresi.text = "Vananın Çalışma Süresi: 00:00:00"
            timer?.cancel()
            timer = null
        }
    }
    
    // Zamanlayıcı başlatma fonksiyonu
    private fun baslatzamanlayici() {
        // Önceki zamanlayıcıyı iptal et
        timer?.cancel()
        timer = null
        
        // Yeni zamanlayıcı oluştur
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    if (isVanaAcik) {
                        val gecenSure = System.currentTimeMillis() - vanaSonAcilisSaati
                        tvVanaCalismaSuresi.text = "Vananın Çalışma Süresi: ${formatSure(gecenSure)}"
                    } else {
                        // Vana kapalıysa timer'ı iptal et
                        this.cancel()
                        timer = null
                    }
                }
            }
        }, 0, 1000)
    }
    
    // Süre formatı fonksiyonu
    private fun formatSure(milisaniye: Long): String {
        val seconds = (milisaniye / 1000) % 60
        val minutes = (milisaniye / (1000 * 60)) % 60
        val hours = (milisaniye / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    // Simüle edilmiş sensör verileri (ESP32 entegrasyonu olmadan gösterim için) - Devre dışı bırakıldı
    private fun simuleSensorVerileri() {
        // Gerçek ESP32 bağlantısı olduğu için simülasyon verisi oluşturmaya gerek yok
        println("Gerçek ESP32 cihazı bağlı olduğu için simülasyon devre dışı.")
    }
    
    /**
     * Test sensör verilerini gönder - Gerçek ESP32 bağlantısı olduğu için devre dışı bırakıldı
     */
    private fun sendTestSensorData() {
        // Gerçek ESP32 bağlantısı olduğu için simülasyon verisi göndermeye gerek yok
        println("Gerçek ESP32 cihazı bağlı olduğu için test verileri gönderilmiyor.")
    }
    
    // FloatRange için yardımcı fonksiyon
    private fun ClosedFloatingPointRange<Float>.random() =
        Random().nextFloat() * (endInclusive - start) + start
    
    override fun onPause() {
        super.onPause()
        // Uygulama arka plana alındığında durumu kaydet
        updateSystemStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Zamanlayıcıları temizle
        timer?.cancel()
    }
    
    /**
     * Hava durumu gözlemcilerini ayarla
     */
    private fun setupWeatherObservers() {
        // Hava durumu verileri gözlemcisi
        weatherViewModel.weatherData.observe(this, Observer { weatherInfo ->
            updateWeatherInfo(weatherInfo)
        })
        
        // Hata gözlemcisi
        weatherViewModel.error.observe(this, Observer { errorMsg ->
            // Hata durumunda yeniden deneme
            if (errorMsg.isNotEmpty()) {
                retryWeatherFetch()
            }
        })
        
        // Yükleniyor gözlemcisi
        weatherViewModel.isLoading.observe(this, Observer { isLoading ->
            if (!isLoading) {
                // Yükleme tamamlandığında ve veri varsa güncelleme başarılı olarak işaretle
                if (weatherViewModel.weatherData.value != null) {
                    weatherViewModel.checkAndNotifyWeatherUpdate()
                }
            }
        })
        
        // Güncelleme başarılı gözlemcisi
        weatherViewModel.weatherUpdateSuccess.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Hava durumu bilgileri güncellendi", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    /**
     * Hava durumu verilerini güncelle
     */
    private fun updateWeatherInfo(weatherInfo: WeatherInfo) {
        val locationText = "${weatherInfo.city}, ${weatherInfo.country}"
        tvLocation.text = locationText
    }
    
    /**
     * Büyük harfle başlama için extension
     */
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this[0].uppercaseChar() + this.substring(1)
        } else {
            this
        }
    }
    
    /**
     * Düzenli olarak sistem durumunu güncelleyen bir zamanlayıcı başlat
     */
    private fun startSystemStatusChecker() {
        statusTimer?.cancel()
        statusTimer = Timer()
        statusTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    updateSystemStatus()
                }
            }
        }, 0, 10000) // 10 saniyede bir kontrol
    }

    private fun fetchForecastWeather() {
        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = "YOUR_WEATHER_API_KEY"
            val city = "Ankara"
            val country = "TR"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance()
            val todayStr = dateFormat.format(today.time)
            val forecastUrl = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$city,$country&days=5&lang=tr"
            val historyDates = mutableListOf<String>()
            for (i in -2..-1) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, i)
                historyDates.add(dateFormat.format(cal.time))
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            // Forecast verisini hemen çek
            val forecastDeferred = async {
                val request = Request.Builder().url(forecastUrl).build()
                val response = client.newCall(request).execute()
                response.body?.string()
            }
            // History isteklerini paralel başlat
            val historyDeferreds = historyDates.map { date ->
                async {
                    val url = "https://api.weatherapi.com/v1/history.json?key=$apiKey&q=$city,$country&dt=$date&lang=tr"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    response.body?.string()
                }
            }
            // Önce forecast verisini işle ve ekrana göster
            val forecastBody = forecastDeferred.await()
            val tempList = mutableListOf<ForecastDay>()
            var todaySummaryText = ""
            if (forecastBody != null) {
                try {
                    val json = JSONObject(forecastBody)
                    if (json.has("forecast")) {
                        val forecast = json.getJSONObject("forecast").getJSONArray("forecastday")
                        for (i in 0 until forecast.length()) {
                            val dayObj = forecast.getJSONObject(i)
                            val dateStr = dayObj.getString("date")
                            val day = dayObj.getJSONObject("day")
                            val minTemp = day.getDouble("mintemp_c").toInt()
                            val maxTemp = day.getDouble("maxtemp_c").toInt()
                            val condition = day.getJSONObject("condition")
                            val desc = condition.getString("text")
                            val dayName = getDayName(dateStr)
                            val iconResId = getWeatherIconResFromText(desc)
                            tempList.add(ForecastDay(dayName, iconResId, minTemp, maxTemp, desc))
                            if (dateStr == todayStr) {
                                todaySummaryText = "Hava $maxTemp°C, $desc"
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Forecast verisini hemen göster
            withContext(Dispatchers.Main) {
                if (tempList.isNotEmpty()) {
                    forecastList.clear()
                    // Sadece forecast verisiyle 5 kutucuk göster
                    forecastList.addAll(tempList.take(5))
                    forecastAdapter.notifyDataSetChanged()
                    tvTodaySummary.text = todaySummaryText
                    // Ortadaki kutucuğu (bugün) ortala
                    rvForecast.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            rvForecast.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val layoutManager = rvForecast.layoutManager as LinearLayoutManager
                            val middle = 2
                            val itemWidth = rvForecast.getChildAt(0)?.width ?: 0
                            val recyclerWidth = rvForecast.width
                            val offset = (recyclerWidth / 2) - (itemWidth / 2)
                            layoutManager.scrollToPositionWithOffset(middle, offset)
                        }
                    })
                }
            }
            // Sonra history verilerini bekle ve ekrana ekle
            val historyBodies = historyDeferreds.awaitAll()
            val historyList = mutableListOf<ForecastDay>()
            for ((i, body) in historyBodies.withIndex()) {
                if (body != null) {
                    try {
                        val json = JSONObject(body)
                        if (json.has("forecast")) {
                            val dayObj = json.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0)
                            val day = dayObj.getJSONObject("day")
                            val minTemp = day.getDouble("mintemp_c").toInt()
                            val maxTemp = day.getDouble("maxtemp_c").toInt()
                            val condition = day.getJSONObject("condition")
                            val desc = condition.getString("text")
                            val dayName = getDayName(historyDates[i])
                            val iconResId = getWeatherIconResFromText(desc)
                            historyList.add(ForecastDay(dayName, iconResId, minTemp, maxTemp, desc))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            // History verileri geldiyse forecast ile birleştirip güncelle
            if (historyList.size == 2 && tempList.isNotEmpty()) {
                val allDays = historyList + tempList.take(3)
                withContext(Dispatchers.Main) {
                    forecastList.clear()
                    forecastList.addAll(allDays)
                    forecastAdapter.notifyDataSetChanged()
                    tvTodaySummary.text = todaySummaryText
                    // Ortadaki kutucuğu (bugün) ortala
                    rvForecast.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            rvForecast.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val layoutManager = rvForecast.layoutManager as LinearLayoutManager
                            val middle = 2
                            val itemWidth = rvForecast.getChildAt(0)?.width ?: 0
                            val recyclerWidth = rvForecast.width
                            val offset = (recyclerWidth / 2) - (itemWidth / 2)
                            layoutManager.scrollToPositionWithOffset(middle, offset)
                        }
                    })
                }
            }
        }
    }

    private fun getDayName(dateStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr)
        val cal = java.util.Calendar.getInstance()
        cal.time = date!!
        val days = arrayOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
        return days[cal.get(java.util.Calendar.DAY_OF_WEEK)-1]
    }

    private fun getWeatherIconResFromText(desc: String): Int {
        val d = desc.lowercase(Locale.getDefault())
        return when {
            d.contains("güneş") || d.contains("açık") -> R.drawable.ic_weather_clear
            d.contains("bulutlu") && d.contains("parçalı") -> R.drawable.ic_weather_partly_cloudy
            d.contains("bulutlu") -> R.drawable.ic_weather_cloudy
            d.contains("yağmur") || d.contains("sağanak") -> R.drawable.ic_weather_rain
            d.contains("fırtına") -> R.drawable.ic_weather_storm
            d.contains("kar") -> R.drawable.ic_weather_snow
            d.contains("sis") -> R.drawable.ic_weather_fog
            else -> R.drawable.ic_weather_cloudy
        }
    }

    private fun retryWeatherFetch() {
        if (weatherRetryCount < MAX_RETRY_COUNT) {
            weatherRetryCount++
            Handler(Looper.getMainLooper()).postDelayed({
                weatherViewModel.fetchWeather()
                fetchForecastWeather()
            }, RETRY_DELAY)
        } else {
            weatherRetryCount = 0
            Toast.makeText(this, "Hava durumu bilgileri alınamadı. Lütfen internet bağlantınızı kontrol edin.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showVanaAlert(message: String) {
        val titleView = TextView(this)
        titleView.text = "Uyarı"
        titleView.setPadding(32, 32, 32, 16)
        titleView.textSize = 20f
        titleView.setTextColor(resources.getColor(R.color.primary_green, null))
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        titleView.gravity = android.view.Gravity.CENTER
        titleView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setMessage(message)
            .setPositiveButton("Evet") { _, _ -> vanaAc() }
            .setNegativeButton("Hayır", null)
            .create()
        dialog.show()
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.primary_green, null))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.primary_green, null))
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        val messageView = dialog.findViewById<TextView>(android.R.id.message)
        messageView?.setTextColor(resources.getColor(R.color.text_dark, null))
        messageView?.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        messageView?.gravity = android.view.Gravity.CENTER
    }

    // Vana açılırken başlangıç verilerini kaydet
    private fun logVanaAcildi() {
        logBaslangicSicaklik = sicaklik
        logBaslangicNem = nem
        logBaslangicToprakNem = tvToprakNem.text.toString()
        logBaslangicYagmur = tvYagmur.text.toString()
        logBaslangicSu = suMiktari
        // ... mevcut log işlemleri ...
    }

    // Vana kapanınca logda hem başlangıç hem bitiş verilerini göster
    private fun logVanaKapatildi(kapatmaZamani: Long, sulamaSuresi: Long) {
        val kapanisDetaylari = """
Sulama süresi: ${LogManager.formatDuration(sulamaSuresi)}

Başlangıç Değerleri:
Sıcaklık: ${logBaslangicSicaklik.toInt()}°C
Hava Nemi: ${logBaslangicNem.toInt()}%
Toprak Nem: $logBaslangicToprakNem
Yağmur: $logBaslangicYagmur
Akan Su: ${String.format("%.2f L/s", logBaslangicSu)}

Bitiş Değerleri:
Sıcaklık: ${sicaklik.toInt()}°C
Hava Nemi: ${nem.toInt()}%
Toprak Nem: ${tvToprakNem.text}
Yağmur: ${tvYagmur.text}
Akan Su: ${String.format("%.2f L/s", suMiktari)}
""".trimIndent()
        LogManager.addLog(LogEntry(
            message = "Vana kapatıldı",
            timestamp = kapatmaZamani,
            type = LogType.INFO,
            details = kapanisDetaylari
        ))
        LogManager.saveLogsToStorage(this)
    }
}