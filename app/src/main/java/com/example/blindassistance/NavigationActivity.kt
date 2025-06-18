package com.example.blindassistance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.*

class NavigationActivity : AppCompatActivity(), TextToSpeech.OnInitListener, OnMapReadyCallback {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var googleMap: GoogleMap
    private lateinit var fromLocation: LatLng
    private lateinit var toLocation: LatLng

    companion object {
        private const val VOICE_INPUT_REQUEST = 100
        private const val LOCATION_PERMISSION_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        textToSpeech = TextToSpeech(this, this)

        checkLocationPermission()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageView>(R.id.voice_input).setOnClickListener {
            startVoiceRecognition()
        }

        findViewById<Button>(R.id.start_navigation).setOnClickListener {
            if (::fromLocation.isInitialized && ::toLocation.isInitialized) {
                startNavigation()
            } else {
                speak("Please provide both starting and destination addresses.")
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                speak("Location permission granted.")
            } else {
                speak("Location permission denied. Navigation may not work properly.")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
            speak("Navigation mode activated. Tap the mic icon and say your starting and destination addresses.")
        }
    }

    private fun speak(message: String) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        try {
            startActivityForResult(intent, VOICE_INPUT_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_INPUT_REQUEST && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { processVoiceCommand(it) }
        }
    }

    private fun processVoiceCommand(command: String) {
        when {
            command.equals("go to main menu", ignoreCase = true) -> {
                speak("Return")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            command.contains("to", ignoreCase = true) -> {
                val locations = command.split("to")
                if (locations.size == 2) {
                    val fromAddress = locations[0].trim()
                    val toAddress = locations[1].trim()

                    speak("Navigating from $fromAddress to $toAddress.")

                    fromLocation = getLocationFromAddress(fromAddress)
                    toLocation = getLocationFromAddress(toAddress)

                    if (::fromLocation.isInitialized && ::toLocation.isInitialized) {
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(fromLocation).title("Start"))
                        googleMap.addMarker(MarkerOptions().position(toLocation).title("Destination"))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fromLocation, 12f))
                    } else {
                        speak("Could not find locations. Please try again.")
                    }
                }
            }
            else -> {
                speak("Command not recognized. Please try again.")
            }
        }
    }

    private fun getLocationFromAddress(address: String): LatLng {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList: List<Address> = geocoder.getFromLocationName(address, 1) ?: emptyList()

            if (addressList.isNotEmpty()) {
                LatLng(addressList[0].latitude, addressList[0].longitude)
            } else {
                LatLng(0.0, 0.0) // Invalid location
            }
        } catch (e: IOException) {
            speak("Error retrieving location.")
            LatLng(0.0, 0.0)
        }
    }

    private fun startNavigation() {
        val gmmIntentUri = Uri.parse("google.navigation:q=${toLocation.latitude},${toLocation.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            speak("Starting navigation.")
            startActivity(mapIntent)
        } else {
            speak("Google Maps is not installed.")
        }
    }



    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
