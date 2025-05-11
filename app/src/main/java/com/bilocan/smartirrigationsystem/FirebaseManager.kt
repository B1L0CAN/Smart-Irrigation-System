package com.bilocan.smartirrigationsystem

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

/**
 * ESP32 ile Firebase Realtime Database üzerinden haberleşmeyi sağlayan sınıf
 */
class FirebaseManager private constructor() {
    
    // Firebase Realtime Database URL
    private val DB_URL = "https://smartirrigationsystem-bfff8-default-rtdb.europe-west1.firebasedatabase.app"
    
    // Firebase database instance
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(DB_URL)
    
    // Database referansları
    private val sensorRef: DatabaseReference = database.getReference("sensors")
    private val controlRef: DatabaseReference = database.getReference("control")
    private val valveRef: DatabaseReference = database.getReference("valve")
    private val esp32StatusRef: DatabaseReference = database.getReference("esp32_status")
    
    init {
        // Daha az veritabanı okuma için disk önbelleğini etkinleştir
        sensorRef.keepSynced(true)
        controlRef.keepSynced(true)
    }
    
    companion object {
        @Volatile
        private var instance: FirebaseManager? = null
        
        fun getInstance(): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager().also { instance = it }
            }
        }
    }
    
    /**
     * Sensör verilerini dinlemek için kullanılır - Genişletilmiş versiyonu
     */
    fun listenToSensorData(
        onDataReceived: (
            temperature: Float,
            humidity: Float,
            isSoilWet: Boolean,
            isRaining: Boolean,
            flowRate: Float,
            timestamp: Long
        ) -> Unit,
        onError: (error: String) -> Unit
    ) {
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val temperature = snapshot.child("temperature").getValue(Float::class.java) ?: 0.0f
                    val humidity = snapshot.child("humidity").getValue(Float::class.java) ?: 0.0f
                    val isSoilWet = snapshot.child("isSoilWet").getValue(Boolean::class.java) ?: false
                    val isRaining = snapshot.child("isRaining").getValue(Boolean::class.java) ?: false
                    val flowRate = snapshot.child("flowRateLpm").getValue(Float::class.java) ?: 0.0f
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    onDataReceived(temperature, humidity, isSoilWet, isRaining, flowRate, timestamp)
                } catch (e: Exception) {
                    onError("Veri okuma hatası: ${e.message}")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                onError("Firebase hatası: ${error.message}")
            }
        })
    }
    
    /**
     * ESP32'den sensör verilerini güncellemek için kullanılır
     */
    fun updateSensorData(
        sensorData: Map<String, Any>,
        onSuccess: (() -> Unit)? = null,
        onError: ((error: String) -> Unit)? = null
    ) {
        // Firebase'e debug log ekleyelim
        println("Sensör verileri güncelleniyor: $sensorData")
        
        sensorRef.setValue(sensorData)
            .addOnSuccessListener {
                println("Sensör verileri güncellendi")
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                println("Sensör verileri güncellenemedi: ${e.message}")
                onError?.invoke("Sensör verileri güncellenemedi: ${e.message}")
            }
    }
    
    /**
     * Vana durumunu dinlemek için kullanılır
     */
    fun listenToValveState(
        onValveStateChanged: (isOpen: Boolean) -> Unit,
        onError: (error: String) -> Unit
    ) {
        controlRef.child("valve").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val isOpen = snapshot.child("isOpen").getValue(Boolean::class.java) ?: false
                    onValveStateChanged(isOpen)
                } catch (e: Exception) {
                    onError("Vana durumu okuma hatası: ${e.message}")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                onError("Firebase hatası: ${error.message}")
            }
        })
    }
    
    /**
     * ESP32'ye vana kontrolü için komut gönderir
     */
    fun setValveState(isOpen: Boolean, onSuccess: () -> Unit, onError: (error: String) -> Unit) {
        val valveData = mapOf(
            "isOpen" to isOpen,
            "timestamp" to Date().time,
            "updatedBy" to "Android"
        )
        
        // Firebase'e debug log ekleyelim
        println("Firebase'e veri gönderiliyor: $valveData")
        
        controlRef.child("valve").setValue(valveData)
            .addOnSuccessListener { 
                println("Firebase vana durumu güncellendi: $isOpen")
                onSuccess() 
            }
            .addOnFailureListener { e -> 
                println("Firebase hatası: ${e.message}")
                onError("Vana durumu güncellenemedi: ${e.message}") 
            }
    }
    
    /**
     * ESP32'nin bağlantı durumunu kontrol eder
     */
    fun checkESP32Connection(
        onConnected: (lastSeen: Long) -> Unit,
        onDisconnected: () -> Unit
    ) {
        val connRef = database.getReference("devices/esp32/status")
        connRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("online").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                
                if (status) {
                    onConnected(lastSeen)
                } else {
                    onDisconnected()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                onDisconnected()
            }
        })
    }
    
    /**
     * Otomatik sulama ayarlarını günceller
     */
    fun updateAutoIrrigationSettings(
        enabled: Boolean,
        humidityThreshold: Int,
        wateringDuration: Int,
        onSuccess: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        val settingsData = mapOf(
            "enabled" to enabled,
            "humidityThreshold" to humidityThreshold,
            "wateringDuration" to wateringDuration,
            "updatedAt" to Date().time
        )
        
        controlRef.child("autoIrrigation").setValue(settingsData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError("Ayarlar güncellenemedi: ${e.message}") }
    }
    
    /**
     * ESP32 durumunu güncelle
     */
    fun setESP32Status(
        statusData: Map<String, Any>, 
        onSuccess: (() -> Unit)? = null, 
        onError: ((error: String) -> Unit)? = null
    ) {
        val connRef = database.getReference("devices/esp32/status")
        
        // Firebase'e debug log ekleyelim
        println("ESP32 status güncelleniyor: $statusData")
        
        connRef.setValue(statusData)
            .addOnSuccessListener { 
                println("ESP32 status güncellendi")
                onSuccess?.invoke() 
            }
            .addOnFailureListener { e -> 
                println("ESP32 status güncellenemedi: ${e.message}")
                onError?.invoke("ESP32 status güncellenemedi: ${e.message}") 
            }
    }

    fun listenToESP32Status(
        onStatusChanged: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        esp32StatusRef.child("isActive").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val isActive = snapshot.getValue(Boolean::class.java) ?: false
                    onStatusChanged(isActive)
                } catch (e: Exception) {
                    onError("ESP32 durum verisi okunamadı: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError("ESP32 durum dinleme iptal edildi: ${error.message}")
            }
        })
    }
} 