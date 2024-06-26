package com.example.crowdsense

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.greenrobot.eventbus.EventBus
import java.lang.Exception

class LocationService : Service() {

    companion object{
        const val CHANNEL_ID="12345"
        const val NOTIFICATION_ID = 12345
    }

    private var fusedLocationProviderClient : FusedLocationProviderClient?=null
    private var locationCallback:LocationCallback?=null
    private var locationRequest:LocationRequest?=null
    private var notificationManager:NotificationManager?=null

    private var location: Location?=null

    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000).setIntervalMillis(1000).build()
        locationCallback = object: LocationCallback(){
            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)
            }

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult)

            }
        }
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(CHANNEL_ID,"locations",NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    @Suppress("MissingPermission")
    fun createLocationRequest(){
        try{
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest!!,locationCallback!!,null
            )
        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun removeLocationUpdates(){
        locationCallback?.let{
            fusedLocationProviderClient?.removeLocationUpdates(it)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onNewLocation(locationResult: LocationResult) {
        location = locationResult.lastLocation
        EventBus.getDefault().post(location?.longitude?.let {
            LocationEvent(
                latitude = location?.latitude,
                longitude = it
            )
        })
        startForeground(NOTIFICATION_ID,getNotification())
    }

    fun getNotification(): Notification {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Location Updates").setContentText(
            "Latitute --> ${location?.latitude}\nLongitute --> ${location?.longitude}"
        ).setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            notification.setChannelId(CHANNEL_ID)
        }
        return notification.build()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createLocationRequest()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder?= null

    override fun onDestroy() {
        super.onDestroy()
        removeLocationUpdates()
    }
}