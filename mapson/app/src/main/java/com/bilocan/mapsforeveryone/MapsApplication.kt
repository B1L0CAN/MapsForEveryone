package com.bilocan.mapsforeveryone

import android.app.Application
import com.bilocan.mapsforeveryone.api.RetrofitClient

class MapsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // RetrofitClient'a context sağla
        RetrofitClient.init(this)
    }
} 