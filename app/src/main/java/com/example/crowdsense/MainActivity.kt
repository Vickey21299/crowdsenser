package com.example.crowdsense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activtity_main)
        var receiver = findViewById<Button>(R.id.receiver)
        var transmitter = findViewById<Button>(R.id.transmitter)

        transmitter.setOnClickListener{
            startActivity(Intent(this,MainActivity1::class.java))

        }
        receiver.setOnClickListener {
            startActivity(Intent(this,MainActivity1::class.java))
        }

    }
}

