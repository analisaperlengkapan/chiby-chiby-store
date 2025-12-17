package com.chibychibystore

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class untuk Chiby Chiby Store
 * 
 * Class ini adalah entry point untuk aplikasi dan digunakan untuk
 * inisialisasi Hilt dependency injection.
 */
@HiltAndroidApp
class ChibyChibyStoreApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}
