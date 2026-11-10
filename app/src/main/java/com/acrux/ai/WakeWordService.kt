package com.acrux.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback

class WakeWordService : Service() {

    private var porcupineManager: PorcupineManager? = null
    private var tts: AcruxTTS? = null
    private val CHANNEL_ID = "acrux_wake_word_channel"
    private val NOTIFICATION_ID = 1001

    // TODO: Yahan apni Picovoice AccessKey daalna
    private val ACCESS_KEY = "YOUR_PICOVOICE_ACCESS_KEY_HERE"

    override fun onCreate() {
        super.onCreate()
        tts = AcruxTTS(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        try {
            val callback = PorcupineManagerCallback { keywordIndex ->
                Log.d("ACRUX", "Wake word detected! Index: $keywordIndex")
                tts?.speak("Yes, Boss. ACRUX is here. What can I help you with?")
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath("hey_acrux.ppn") 
                .setSensitivity(0.6f)
                .build(applicationContext, callback)

            porcupineManager?.start()
            Log.d("ACRUX", "Wake word service started successfully")

        } catch (e: PorcupineException) {
            Log.e("ACRUX", "Porcupine init failed: ${e.message}")
            tts?.speak("I could not start listening, Boss. Please check the wake word setup.")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
        } catch (e: Exception) {
            Log.e("ACRUX", "Error stopping Porcupine: ${e.message}")
        }
        tts?.shutdown()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ACRUX Always Ready",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ACRUX listening for wake word"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ACRUX is ready")
                .setContentText("Listening for 'Hey ACRUX'")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("ACRUX is ready")
                .setContentText("Listening for 'Hey ACRUX'")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        }
    }
}
