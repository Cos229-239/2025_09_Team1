package com.example.wildercards

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "onCreate - Before FirebaseApp.initializeApp")
        FirebaseApp.initializeApp(this)
        Log.d("MyApplication", "onCreate - After FirebaseApp.initializeApp")
    }
}
