package uz.faylqalqoni.shield

import android.app.Application
import uz.faylqalqoni.shield.notification.NotificationHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
