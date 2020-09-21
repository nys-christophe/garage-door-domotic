package com.isodroid.portedegarage.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.isodroid.portedegarage.receiver.RestartBackgroundService
import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.location.*
import com.isodroid.portedegarage.BuildConfig
import com.isodroid.portedegarage.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import com.isodroid.portedegarage.R

class LocationService : Service() {

    companion object {
        var outside: Boolean = false
    }

    val NOTIFICATION_CHANNEL_ID = "com.getlocationbackground"

    var counter = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var firstTime : Boolean = true

    var notifcount = 100

    private val TAG = "LocationService"

    override fun onCreate() {
        super.onCreate()
        createNotificationChanel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChanel() {

        firstTime = true


        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)


        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("Surveillance de la porte de garage")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_car)
            .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        firstTime = true
        outside = false
        stoptimertask()

        if (MainActivity.restart) {
            val broadcastIntent = Intent()
            broadcastIntent.action = "restartservice"
            broadcastIntent.setClass(this, RestartBackgroundService::class.java)
            this.sendBroadcast(broadcastIntent)
        }
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                var count = counter++
                Log.d("Location Service", "Distance  test")

                GlobalScope.launch(Dispatchers.Main) {
                    requestLocationUpdates()
                }
            }
        }
        timer!!.schedule(
            timerTask,
            0,
            10000
        ) //1 * 60 * 1000 1 minute
    }

    fun stoptimertask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun triggerDoor2(context: Context) {
        Fuel.get(BuildConfig.URL_OPEN_CLOSE)
            .responseString { request, response, result ->
                when (result) {
                    is com.github.kittinunf.result.Result.Failure -> {
                        val ex = result.getException()
                        Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                    }
                    is com.github.kittinunf.result.Result.Success -> {
                        val data = result.get()
                        if (data == "Open/Close") {
                            Toast.makeText(context, "OK", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "KO !!!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

        // Ajoute une notification
        addActionNotification()
        // Arrete le service
        MainActivity.restart = false
        val mLocationService = LocationService()
        val mServiceIntent = Intent(this, mLocationService.javaClass)
        stopService(mServiceIntent)
    }


    private fun addActionNotification() {
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification2: Notification = notificationBuilder
            .setContentTitle("Ouverture du garage")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_car)
            .build()
        manager.notify(notifcount++, notification2)
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 1000
        request.numUpdates = 1
        request.fastestInterval = 10000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {


                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location = locationResult.lastLocation
                    latitude = location.latitude
                    longitude = location.longitude
                    Log.d("Location Service", "location update $location")

                    val results = FloatArray(1)
                    Location.distanceBetween(
                        49.494707,
                        1.144681,
                        latitude,
                        longitude,
                        results
                    )

                    Log.d("Location Service", "Distance ${results[0]}")
                    if (firstTime) {
                        outside = results[0] >= 100
                        firstTime = false
                    }

                    if (results[0] < 100 && outside) {
                        Log.d("Location Service", "Distance GARAGE !!")
                        // J'étais sorti et je rerentre
                        // Déclenche l'ouverture
                        Toast.makeText(
                            this@LocationService,
                            "J ouvre le garage !!!",
                            Toast.LENGTH_LONG
                        ).show()
                        triggerDoor2(this@LocationService)
                        outside = false
                    }
                    if (results[0] > 150) {
                        if (!outside) {
                            Log.d("Location Service", "Distance Outside = TRUE !!")
                        }
                        outside = true
                    }
                }
            }, null)
        }
    }
}