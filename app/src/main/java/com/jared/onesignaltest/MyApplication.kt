package com.jared.onesignaltest

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable verbose OneSignal logging for debugging
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Initialize OneSignal with your App ID
        OneSignal.initWithContext(this, "4dc8fa38-f480-4cee-bc94-72774104d411")

        // Request notification permission (Android 13+)
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}