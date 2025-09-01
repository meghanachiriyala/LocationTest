package com.example.locationtest

import android.app.Application

class LocationTestAPP : Application() {
    companion object {
        lateinit var repository: Repository
            private set
    }

    override fun onCreate() {
        super.onCreate()
        repository =Repository(this)
    }
}