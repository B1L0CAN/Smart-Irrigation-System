package com.bilocan.smartirrigationsystem

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

/**
 * Uygulama başlatıldığında çalışacak olan Firebase ve diğer servisleri
 * yapılandıran ana Application sınıfı
 */
class IrrigationApp : Application() {
    
    companion object {
        // Firebase Realtime Database URL
        private const val DB_URL = "YOUR_FIREBASE_DATABASE_URL"
        private const val API_KEY = "YOUR_FIREBASE_API_KEY"
        private const val APP_ID = "YOUR_FIREBASE_APP_ID"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Firebase'i başlat
            val options = FirebaseOptions.Builder()
                .setDatabaseUrl(DB_URL)
                .setApiKey(API_KEY)
                .setApplicationId(APP_ID)
                .build()
                
            // Eğer halihazırda başlatılmamışsa
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options)
                // Offline kalıcılık BİR KEZ burada ayarlanmalı, başka hiçbir yerde değil
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            }
            
            println("Firebase başarıyla başlatıldı")
        } catch (e: Exception) {
            println("Firebase başlatma hatası: ${e.message}")
            e.printStackTrace()
        }
    }
} 