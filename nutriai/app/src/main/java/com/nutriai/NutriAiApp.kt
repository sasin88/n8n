package com.nutriai

import android.app.Application
import com.nutriai.data.reminders.Notifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NutriAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
    }
}
