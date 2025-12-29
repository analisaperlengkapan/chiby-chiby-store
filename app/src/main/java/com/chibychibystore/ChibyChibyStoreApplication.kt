package com.chibychibystore

import android.app.Application
import com.chibychibystore.service.DataSeedingService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class untuk Chiby Chiby Store
 * 
 * Class ini adalah entry point untuk aplikasi dan digunakan untuk
 * inisialisasi Hilt dependency injection.
 */
@HiltAndroidApp
class ChibyChibyStoreApplication : Application() {
    
    @Inject
    lateinit var dataSeedingService: DataSeedingService
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Check if this is first run and seed initial data
        val prefs = getSharedPreferences("chiby_chiby_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        
        if (isFirstRun) {
            applicationScope.launch {
                try {
                    dataSeedingService.seedInitialData()
                    prefs.edit().putBoolean("is_first_run", false).apply()
                } catch (e: Exception) {
                    // Log error but don't crash
                    e.printStackTrace()
                }
            }
        }
    }
}
