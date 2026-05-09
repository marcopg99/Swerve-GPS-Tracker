package com.mpg99.gpstracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.MainActivity
import com.mpg99.gpstracker.R
import com.mpg99.gpstracker.data.TrackPoint
import com.mpg99.gpstracker.util.formatDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var insertJob: Job? = null
    private var currentTrackId: Long = -1L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val app = applicationContext as GPSTrackerApp
            val trackId = currentTrackId.takeIf { it > 0 } ?: return
            val now = System.currentTimeMillis()
            val points = result.locations.map { loc ->
                TrackPoint(
                    trackId = trackId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude.takeIf { loc.hasAltitude() },
                    accuracy = loc.accuracy.takeIf { loc.hasAccuracy() },
                    speed = loc.speed.takeIf { loc.hasSpeed() },
                    bearing = loc.bearing.takeIf { loc.hasBearing() },
                    timestamp = if (loc.time > 0) loc.time else now
                )
            }
            scope.launch {
                points.forEach { app.repository.addPoint(it) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        startForegroundCompat()

        val app = applicationContext as GPSTrackerApp
        scope.launch {
            val now = System.currentTimeMillis()
            val name = "Ruta ${formatDateTime(now)}"
            val id = app.repository.startTrack(name, now)
            currentTrackId = id
            _state.value = TrackingState(isTracking = true, trackId = id, startedAt = now)
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(LOCATION_MIN_DISTANCE_M)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        val app = applicationContext as GPSTrackerApp
        val finishedId = currentTrackId
        currentTrackId = -1L
        _state.value = TrackingState()
        if (finishedId > 0) {
            scope.launch {
                app.repository.finishTrack(finishedId, System.currentTimeMillis())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        insertJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, TrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "gps_tracking"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.mpg99.gpstracker.action.START"
        const val ACTION_STOP = "com.mpg99.gpstracker.action.STOP"

        private const val LOCATION_INTERVAL_MS = 5_000L
        private const val LOCATION_FASTEST_INTERVAL_MS = 2_000L
        private const val LOCATION_MIN_DISTANCE_M = 5f

        private val _state = MutableStateFlow(TrackingState())
        val state: StateFlow<TrackingState> = _state.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}

data class TrackingState(
    val isTracking: Boolean = false,
    val trackId: Long = -1L,
    val startedAt: Long = 0L
)
