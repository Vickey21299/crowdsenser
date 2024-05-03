package com.example.crowdsense

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.crowdsense.databinding.ActivityMain1Binding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity1 : AppCompatActivity() {
    private var _binding: ActivityMain1Binding? = null
    private val binding: ActivityMain1Binding
        get() = _binding!!

//    private val serverURL = "http://192.168.1.9:8000/"
//    private lateinit var location_api : locationdevice_api

    object LocationDataHolder {
        var latitude: String = ""
        var longitude: String = ""
    }


    private var service: Intent?=null
    private lateinit var dbRef: DatabaseReference


    //stopwatch
    lateinit var dataHelper: DataHelper
    private val timer = Timer()

    private val backgroundLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it){

        }
    }
    companion object{
        var latitudeString : String? = null
        var longitudeString : String? = null
    }


    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        when{
            it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,false)->{

                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                    if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                        backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val firebase : DatabaseReference = FirebaseDatabase.getInstance().getReference("GPS")
        _binding = ActivityMain1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        dataHelper = DataHelper(applicationContext)
        binding.startButton.setOnClickListener{ startStopAction()}
        binding.resetButton.setOnClickListener{ resetAction() }
        if(dataHelper.timerCounting()){
            startTimer()
        }

        else{
            stopTimer()
            if(dataHelper.startTime()!=null && dataHelper.stopTime()!=null){
                val time = Date().time - calcRestartTime().time
                binding.timeTV.text = timeStringFromLong(time)
            }
        }
        timer.scheduleAtFixedRate(TimeTask(),0,500)

        service = Intent(this,LocationService::class.java)
//        setContentView(R.layout.activity_main)
        binding.apply{
            btnStartLocationTracking.setOnClickListener {
                checkPermissions()
            }
            btnRemoveLocationTracking.setOnClickListener {
                stopService(service)
            }
        }
        dbRef= Firebase.database.reference
        val LocationEvent = LocationEvent(latitude = 12.3, longitude = 23.3)
        dbRef.child("io").setValue(LocationEvent).addOnSuccessListener {
            Toast.makeText(this,"Success",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this,"fail",Toast.LENGTH_SHORT).show()

        }

    }

    private inner class TimeTask: TimerTask(){
        override fun run(){
            if(dataHelper.timerCounting()){
                val time = Date().time - dataHelper.startTime()!!.time
                binding.timeTV.text = timeStringFromLong(time)
            }
        }
    }

    private fun resetAction(){
        dataHelper.setStopTime(null)
        dataHelper.setStartTime(null)
        stopTimer()
        binding.timeTV.text = timeStringFromLong(0)
    }

    private fun stopTimer(){
        dataHelper.setTimerCounting(false)
        binding.startButton.text = getString(R.string.start)
    }

    private fun startTimer(){
        dataHelper.setTimerCounting(true)
        binding.startButton.text = getString(R.string.stop)
    }

    private fun startStopAction(){
        if(dataHelper.timerCounting()){
            dataHelper.setStopTime(Date())
            stopTimer()
        }
        else{
            if(dataHelper.stopTime()!=null){
                dataHelper.setStartTime(calcRestartTime())
                dataHelper.setStopTime(null)
            }
            else{
                dataHelper.setStartTime(Date())
            }
            startTimer()
        }

    }

    private fun calcRestartTime(): Date {
        val diff = dataHelper.startTime()!!.time - dataHelper.stopTime()!!.time
        return Date(System.currentTimeMillis()+diff)
    }

    private fun timeStringFromLong(ms: Long):String?{
        val seconds = (ms/1000)%60
        val minutes = (ms/(1000*60)%60)
        val hours = (ms / (1000 * 60 *60) %24)
        return makeTimeString(hours, minutes, seconds)
    }

    private fun makeTimeString(hours: Long, minutes: Long, seconds: Long): String? {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this)
        }
    }

    fun checkPermissions(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                    locationPermissions.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
            }
            else{
                startService(service)
                val intent = Intent(this, MainActivity2::class.java)
                // start your next activity
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(service)
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this)
        }
    }
    @Subscribe
 fun receiveLocationEvent(locationEvent: LocationEvent){
        binding.tvLatitude.text = "Latitude -> ${locationEvent.latitude}"
        binding.tvLongitude.text = "Longitude -> ${locationEvent.longitude}"
         latitudeString = locationEvent.latitude.toString()
         longitudeString = locationEvent.longitude.toString()
        // Start MainActivity2 with intent containing latitudeString and longitudeString
        LocationDataHolder.latitude = locationEvent.latitude.toString()
        LocationDataHolder.longitude = locationEvent.longitude.toString()

    }

}