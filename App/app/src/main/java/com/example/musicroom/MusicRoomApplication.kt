package com.example.musicroom

import android.app.Application
import com.example.musicroom.data.network.NetworkConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicRoomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkConfig.init(this)
    }
}
