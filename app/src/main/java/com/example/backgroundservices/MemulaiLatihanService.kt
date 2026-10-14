package com.example.backgroundservices

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

class MemulaiLatihanService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var stepCount = 0
    private var jumpCount = 0
    private var lastStepTime: Long = 0

    private val NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING = "latihan_service_running"
    private val NOTIFICATION_CHANNEL_ID_LATIHAN_SELESAI = "latihan_selesai_channel"
    private val NOTIFICATION_ID_SERVICE_RUNNING = 10
    private val NOTIFICATION_ID_LATIHAN_SELESAI = 11

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
        Log.d("MemulaiLatihanService", "Service created")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING,
                "Latihan Service Running Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val selesaiChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_LATIHAN_SELESAI,
                "Latihan Selesai Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(selesaiChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MemulaiLatihanService", "Service onStartCommand")

        startForeground(
            NOTIFICATION_ID_SERVICE_RUNNING,
            buildServiceRunningNotification()
        )

        startStepDetection()
        return START_STICKY
    }

    private fun startStepDetection() {
        stepCount = 0
        jumpCount = 0
        accelerometer?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun buildServiceRunningNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING)
            .setContentTitle("Latihan Sedang Berjalan")
            .setContentText("Aplikasi memantau langkah dan lompatan Anda...")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]
            val stepThreshold = 12.0
            val jumpThreshold = 15.0
            val currentTime = System.currentTimeMillis()

            if (abs(y) > stepThreshold && (currentTime - lastStepTime > 500)) {
                stepCount++
                if (abs(y) > jumpThreshold) {
                    jumpCount++
                    Log.d("MemulaiLatihanService", "Lompatan Terdeteksi! Total Lompatan: $jumpCount")
                } else {
                    Log.d("MemulaiLatihanService", "Langkah Terdeteksi! Total Langkah: $stepCount")
                }
                lastStepTime = currentTime

                if (stepCount > 2) {
                    sensorManager.unregisterListener(this)
                    showLatihanSelesaiNotification()

                    val openAppIntent = Intent(this, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_LATIHAN_SELESAI
                        putExtra(MainActivity.EXTRA_STEP_COUNT, stepCount)
                        putExtra(MainActivity.EXTRA_JUMP_COUNT, jumpCount)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(openAppIntent)

                    stopSelf()
                }
            }
        }
    }

    private fun showLatihanSelesaiNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_LATIHAN_SELESAI
            putExtra(MainActivity.EXTRA_STEP_COUNT, stepCount)
            putExtra(MainActivity.EXTRA_JUMP_COUNT, jumpCount)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingMainActivityIntent = PendingIntent.getActivity(
            this,
            2,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val selesaiNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_LATIHAN_SELESAI)
            .setContentTitle("Latihan Selesai!")
            .setContentText("Langkah: $stepCount | Lompatan: $jumpCount. Ketuk untuk membuka aplikasi.")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentIntent(pendingMainActivityIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_LATIHAN_SELESAI, selesaiNotification)
        } catch (e: SecurityException) {
            Log.e(
                "MemulaiLatihanService",
                "Gagal memunculkan notifikasi karena permission POST_NOTIFICATIONS tidak aktif: ${e.message}"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("MemulaiLatihanService", "Service onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
