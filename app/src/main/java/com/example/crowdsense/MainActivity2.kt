package com.example.crowdsense


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

const val REQUEST_ENABLE_BT =1


class MainActivity2 : AppCompatActivity() {
    //BluetoothAdapter
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevices: ArrayAdapter<String>? = null
    var mNameDevices: ArrayAdapter<String>? = null

    private val serverURL = "http://192.168.147.229:8000/"
    private lateinit var location_api : locationdevice_api
    private lateinit var locationString : MainActivity1.Companion
    companion object{
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private var m_bluetoothSocket: BluetoothSocket? = null

        var m_isConnected: Boolean = false
        lateinit var m_address: String
        var lastmessage : String?= null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val latitudeString = MainActivity1.LocationDataHolder.latitude
        val longitudeString = MainActivity1.LocationDataHolder.longitude


//        val latitudeString = intent.getStringExtra("LATITUDE_STRING")
//        val longitudeString = intent.getStringExtra("LONGITUDE_STRING")

        // Display latitude and longitude strings using Toast
        Toast.makeText(this, "Latitude: $latitudeString, Longitude: $longitudeString", Toast.LENGTH_LONG).show()
        //Bluetooth Adapter
        mAddressDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mNameDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        var idBtnOnBT = findViewById<Button>(R.id.idBtnOnBT)
        var idBtnOffBT = findViewById<Button>(R.id.idBtnOffBT)
        var idBtnConnect = findViewById<Button>(R.id.idBtnConnect)
        var idBtnEnviar = findViewById<Button>(R.id.idBtnEnviar)

        var idBtnLuz_1on = findViewById<Button>(R.id.idBtnLuz_1on)
        var idBtnLuz_1off = findViewById<Button>(R.id.idBtnLuz_1off)

        var idBtnLuz_2on = findViewById<Button>(R.id.idBtnLuz_2on)
        var idBtnLuz_2off = findViewById<Button>(R.id.idBtnLuz_2off)

        var idBtnDispBT = findViewById<Button>(R.id.idBtnDispBT)
        var idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
        var idTextOut = findViewById<EditText>(R.id.idTextOut)

        val someActivityResultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if(result.resultCode == REQUEST_ENABLE_BT){
                Log.i(".MainAcitivity","Activity recorded")
            }
        }

        // Initialization of Bluetooth Adapter
        mBtAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if(mBtAdapter == null){
            Toast.makeText(this, "Bluetooth is not available on this device", Toast.LENGTH_LONG).show()
        }
        else{
            Toast.makeText(this,"Bluetooth is available on this device",Toast.LENGTH_LONG).show()
        }


        idBtnOnBT.setOnClickListener{
            if(mBtAdapter.isEnabled){
                Toast.makeText(this, "Bluetooth is already activated", Toast.LENGTH_LONG).show()
            }
            else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if(ActivityCompat.checkSelfPermission(
                        this,Manifest.permission.BLUETOOTH_CONNECT
                    )!= PackageManager.PERMISSION_GRANTED){
                    Log.i("MainAcitivity", "ActivityCompat#requestPermissions")
                }
                someActivityResultLauncher.launch(enableBtIntent)
            }
        }

        idBtnOffBT.setOnClickListener{
            if(!mBtAdapter.isEnabled){
                Toast.makeText(this,"Bluetooth is already deactivated",Toast.LENGTH_LONG).show()
            }
            else{
                mBtAdapter.disable()
                Toast.makeText(this,"Bluetooth has been disabled",Toast.LENGTH_LONG).show()
            }
        }

        idBtnDispBT.setOnClickListener{
            if(mBtAdapter.isEnabled){
                // Check for location permissions
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    // Permission not granted, request it
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        1001)
                } else {
                    // Permission already granted, proceed with Bluetooth scanning operation
                    // Your Bluetooth scanning code here
                }


                val pairedDevices: Set<BluetoothDevice>? = mBtAdapter?.bondedDevices
                mAddressDevices!!.clear()
                mNameDevices!!.clear()

                pairedDevices?.forEach{ device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address //MAC address
                    mAddressDevices!!.add(deviceHardwareAddress)

                    mNameDevices!!.add(deviceName)
                }
                idSpinDisp.setAdapter(mNameDevices)
            }
            else{
                val noDevices = "No device could be paired"
                mAddressDevices!!.add(noDevices)
                mNameDevices!!.add(noDevices)
                Toast.makeText(this, "First pair a bluetooth device",Toast.LENGTH_LONG).show()
            }
        }

        idBtnConnect.setOnClickListener{
            try{
                if(m_bluetoothSocket == null || !m_isConnected){

                    // Cancel discovery because it slows down the connection
                    // Check for location permissions
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                        // Permission not granted, request it
                        ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                            1001)
                    } else {
                        // Permission already granted, proceed with Bluetooth scanning operation
                        // Your Bluetooth scanning code here
                    }

                    val IntValSpin = idSpinDisp.selectedItemPosition
                    m_address = mAddressDevices!!.getItem(IntValSpin).toString()
                    Toast.makeText(this, m_address,Toast.LENGTH_LONG).show()
                    val device: BluetoothDevice = mBtAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    m_bluetoothSocket!!.connect()

                    val incomingMessagesList = mutableListOf<String>()
                    try {
                        val inputStream = m_bluetoothSocket!!.inputStream
                        val buffer = ByteArray(1024)
                        var bytes: Int


                        while (true) {
                            bytes = inputStream.read(buffer)
                            val incomingMessage = String(buffer, 0, bytes)
                            incomingMessagesList.add(incomingMessage) // Add message to the list
                            lastmessage = incomingMessagesList.lastOrNull() // Store the last received message
                            Log.d("Bluetooth", "Received messages:\n$incomingMessage")

                            Toast.makeText(this@MainActivity2, incomingMessage, Toast.LENGTH_LONG).show()
                            // Check if timeout has occurred

//                            var latitudeString = locationString.latitudeString.toString()
//                            var   longitudeString = locationString.longitudeString.toString()
//                            Log.d("jassu", "latitudeString: $latitudeString, longitudeString: $longitudeString, lastmessage: $lastmessage")
//
//                            Toast.makeText(this@MainActivity2, "latitudeString: $latitudeString, longitudeString: $longitudeString, lastmessage: $lastmessage", Toast.LENGTH_LONG).show()

                            lastmessage.toString()
                            val retrofit = Retrofit.Builder()
                                .baseUrl(serverURL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                            location_api = retrofit.create(locationdevice_api::class.java)

                            // Create LocationEvent object with strings
                            val locationEventString = LocationEvent1( lastmessage)

                            // Post location data to the server
                            location_api.add_cred(locationEventString).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    showToast("me haha hu")
                                    if (response.isSuccessful) {
                                        if (response.code() == 200) {
                                            showToast("Data posted successfully")
                                            Log.d("vickeyji", "Data posted successfully")
                                        } else if (response.code() == 201) {
                                            showToast(response.code().toString())
                                            Log.d("vickey", "Data already exists")
                                        }
                                    } else {
                                        showToast("Request failed")
                                        showToast(response.code().toString())
                                        Log.e("vickey", "Request failed: ${response.code()}")
                                    }
                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    Log.e("vickeyji", "Failed to connect to the server", t)
                                    showToast("Failed to connect to the server")
                                }

                            })
                            Thread.sleep(4000)
                        }
                    Toast.makeText(this, "Last message: $lastmessage", Toast.LENGTH_LONG).show()
                    
                    } catch (e: IOException) {
                        // Handle the exception
                        Toast.makeText(this, "Error reading from Bluetooth", Toast.LENGTH_LONG).show()
                    }



                }
                Toast.makeText(this,"Successful connection",Toast.LENGTH_LONG).show()
                Log.i("MainActivity","Successful connection")
            }
            catch (e: IOException){
                // connectSucces = false
                e.printStackTrace()
                Toast.makeText(this,"Connection Error",Toast.LENGTH_LONG).show()
                Log.i("MainActivity","Connection Error")
            }
        }

        idBtnLuz_1on.setOnClickListener{
            sendCommand("A")
        }

        idBtnLuz_1off.setOnClickListener{
            sendCommand("B")
        }
        idBtnLuz_2on.setOnClickListener{
            sendCommand("C")
        }
        idBtnLuz_2off.setOnClickListener{
            sendCommand("D")
        }

        idBtnEnviar.setOnClickListener{
            if(idTextOut.text.toString().isEmpty()){
                Toast.makeText(this,"The name cannot be empty",Toast.LENGTH_SHORT)
            }
            else{
                var message_out: String = idTextOut.text.toString()
                sendCommand(message_out)
            }
        }

    }

    private fun send_to_server() {

    }
    private  fun showToast(message: String){
        Toast.makeText(this , message, Toast.LENGTH_SHORT).show()
    }

    private fun sendCommand(input:String){
        if(m_bluetoothSocket!=null){
            try{
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            }
            catch(e:IOException){
                e.printStackTrace()
            }
        }
    }
}