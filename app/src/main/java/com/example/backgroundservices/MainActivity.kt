package com.example.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var jumlahJumpText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnMulaiLatihan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusTextView = findViewById(R.id.statusTextView)
        jumlahJumpText = findViewById(R.id.jumlahJumpText)
        btnStart = findViewById(R.id.btnStart)
        btnMulaiLatihan = findViewById(R.id.btnMulaiLatihan)

        btnStart.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnMulaiLatihan.setOnClickListener {
            if (hasAllPermissions()) {
                startMemulaiLatihanService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        handleIntent(intent)
    }

    private fun hasAllPermissions(): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService() {
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Background service sedang berjalan..."
    }

    private fun startMemulaiLatihanService() {
        val serviceIntent = Intent(this, MemulaiLatihanService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Service Latihan sedang berjalan. Berjalanlah lebih dari 2 langkah..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                startMemulaiLatihanService()
            } else {
                statusTextView.text = "Izin ditolak, service tidak dapat dijalankan"
            }
        }
    }

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP) {
            statusTextView.text = "Background services sudah berhenti karena Anda melompat"
        } else if (intent?.action == ACTION_LATIHAN_SELESAI) {
            val steps = intent.getIntExtra(EXTRA_STEP_COUNT, 0)
            val jumps = intent.getIntExtra(EXTRA_JUMP_COUNT, 0)

            statusTextView.text = "Latihan Selesai! (Langkah: $steps)"
            jumlahJumpText.text = "Jumlah jump: $jumps"

            createCalendarEvent(steps, jumps)
        }
    }

    private fun createCalendarEvent(steps: Int, jumps: Int) {
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Latihan Hari Ini")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Latihan Selesai! Total Langkah: $steps, Total Lompatan: $jumps"
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + (30 * 60 * 1000))
        }
        try {
            startActivity(calendarIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP = "com.example.backgroundservices.ACTION_SERVICE_STOPPED_JUMP"
        const val ACTION_LATIHAN_SELESAI = "com.example.backgroundservices.ACTION_LATIHAN_SELESAI"
        const val EXTRA_STEP_COUNT = "extra_step_count"
        const val EXTRA_JUMP_COUNT = "extra_jump_count"
        const val REQUEST_CODE_PERMISSIONS = 101
    }
}
