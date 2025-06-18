package com.example.blindassistance

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech

    companion object {
        private const val VOICE_INPUT_REQUEST = 100
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable full-screen mode
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_main)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Speak instructions when home screen opens
        Handler(Looper.getMainLooper()).postDelayed({
            speak("Blind Assistance, Object Detection, Navigation, Document Reader. Tap the mic button to give a voice command.")
        }, 1000) // Delay to allow UI to load

        // Object Detection Button
        findViewById<Button>(R.id.btn_object_detection).setOnClickListener {
            speak("Starting Object Detection")
            startActivity(Intent(this, ObjectDetectionActivity::class.java))
        }

        // Navigation Button
        findViewById<Button>(R.id.btn_navigation).setOnClickListener {
            speak("Starting Navigation")
            startActivity(Intent(this, NavigationActivity::class.java))
        }

        // Document Reader Button
        findViewById<Button>(R.id.btn_document_reader).setOnClickListener {
            speak("Starting Document Reader")
            startActivity(Intent(this, DocumentReaderActivity::class.java))
        }

        // Mic Button (Tap to Activate Voice Commands)
        val micButton = findViewById<ImageButton>(R.id.btn_mic)
        micButton.setOnClickListener {
            speak("Listening for commands")
            startVoiceRecognition()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
        }
    }

    private fun speak(message: String) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Object', 'Navigation', or 'Document'")
        startActivityForResult(intent, VOICE_INPUT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_INPUT_REQUEST && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.lowercase(Locale.getDefault())

            when {
                spokenText?.contains("object") == true -> {
                    speak("Starting Object Detection")
                    startActivity(Intent(this, ObjectDetectionActivity::class.java))
                }
                spokenText?.contains("navigation") == true -> {
                    speak("Starting Navigation")
                    startActivity(Intent(this, NavigationActivity::class.java))
                }
                spokenText?.contains("document") == true || spokenText?.contains("read") == true -> {
                    speak("Starting Document Reader")
                    startActivity(Intent(this, DocumentReaderActivity::class.java))
                }
                else -> speak("Sorry, I didn't understand. Try saying 'Object', 'Navigation', or 'Document'.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
