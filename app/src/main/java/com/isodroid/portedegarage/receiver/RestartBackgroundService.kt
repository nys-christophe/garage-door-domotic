package com.isodroid.portedegarage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.isodroid.portedegarage.service.LocationService

class RestartBackgroundService : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("Broadcast Listened", "Service tried to stop")
        Toast.makeText(context, "Service restarted", Toast.LENGTH_SHORT).show()

        LocationService.outside = false
        context!!.startForegroundService(Intent(context, LocationService::class.java))
    }
}