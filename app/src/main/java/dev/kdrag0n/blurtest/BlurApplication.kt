package dev.kdrag0n.blurtest

import android.app.Application
import timber.log.Timber

class BlurApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}