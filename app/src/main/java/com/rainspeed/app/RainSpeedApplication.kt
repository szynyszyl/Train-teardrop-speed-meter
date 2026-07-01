package com.rainspeed.app

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class RainSpeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val loaded = OpenCVLoader.initLocal()
        Log.i("RainSpeedApplication", "OpenCV native libraries loaded: $loaded")
    }
}
