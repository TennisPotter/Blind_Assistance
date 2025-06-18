package com.example.blindassistance

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream
import java.util.*

class DocumentReaderActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnCapture: Button
    private lateinit var btnSelectImage: Button
    private lateinit var btnReadText: Button
    private lateinit var tvRecognizedText: TextView
    private lateinit var imageView: ImageView
    private lateinit var textToSpeech: TextToSpeech
    private var recognizedText: String = ""

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private const val VOICE_INPUT_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_reader)

        Log.d("DocumentReader", "DocumentReaderActivity Started")

        btnCapture = findViewById(R.id.btnCapture)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnReadText = findViewById(R.id.btnReadText)
        tvRecognizedText = findViewById(R.id.tvRecognizedText)
        imageView = findViewById(R.id.imageView)
        val micButton = findViewById<ImageButton>(R.id.btn_mic)

        textToSpeech = TextToSpeech(this, this)

        btnCapture.setOnClickListener { dispatchTakePictureIntent() }
        btnSelectImage.setOnClickListener { openImagePicker() }
        btnReadText.setOnClickListener {
            if (recognizedText.isNotEmpty()) {
                detectAndSpeakText(recognizedText)
            } else {
                speakText("No text to read. Please capture or select an image first.")
            }
        }
        micButton.setOnClickListener { startVoiceRecognition() }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun openImagePicker() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageIntent.type = "image/*"
        startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(imageBitmap)
                    recognizeTextFromImage(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let { processSelectedImage(it) }
                }
                VOICE_INPUT_REQUEST -> {
                    val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                    handleVoiceCommand(spokenText)
                }
            }
        }
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                recognizedText = visionText.text
                tvRecognizedText.text = recognizedText.ifEmpty { "No text detected" }
                if (recognizedText.isNotEmpty()) {
                    speakTamilText(recognizedText)
                }
            }
            .addOnFailureListener { e ->
                Log.e("DocumentReader", "Text recognition failed: ${e.message}")
                speakText("Text recognition failed. Please try again.")
            }
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bitmap)
            recognizeTextFromImage(bitmap)
        } catch (e: Exception) {
            Log.e("DocumentReader", "Error processing image: ${e.message}")
            speakText("Failed to load image. Please try again.")
        }
    }

    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("capture", ignoreCase = true) || command.contains("scan", ignoreCase = true) -> {
                dispatchTakePictureIntent()
                speakText("Capturing image")
            }
            command.contains("read", ignoreCase = true) || command.contains("text", ignoreCase = true) -> {
                if (recognizedText.isNotEmpty()) {
                    detectAndSpeakText(recognizedText)
                } else {
                    speakText("No text detected. Please capture or select an image first.")
                }
            }
            command.contains("select", ignoreCase = true) || command.contains("gallery", ignoreCase = true) -> {
                openImagePicker()
                speakText("Opening gallery")
            }
            command.contains("exit", ignoreCase = true) || command.contains("back", ignoreCase = true) -> {
                speakText("Exiting document reader")
                finish()
            }
            else -> speakText("Command not recognized. Try saying 'Capture Image', 'Read Text', 'Select Image', or 'Exit'.")
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command")
        startActivityForResult(intent, VOICE_INPUT_REQUEST)
    }

    private fun detectAndSpeakText(text: String) {
        speakTamilText(text) // Tamil text is read in Tamil
    }

    private fun speakText(message: String) {
        textToSpeech.language = Locale.ENGLISH
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakTamilText(message: String) {
        textToSpeech.language = Locale("ta", "IN")
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.ENGLISH
        }
    }
}
