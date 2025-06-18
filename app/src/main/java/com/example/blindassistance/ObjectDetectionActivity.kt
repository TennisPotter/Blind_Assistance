package com.example.blindassistance

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.blindassistance.R.*
import com.example.blindassistance.data.DetectedObjectFirebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectDetectionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var detectedText: TextView
    private lateinit var overlayView: ImageView
    private lateinit var boundingBoxCanvas: ImageView
    private lateinit var switchCameraButton: Button

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetector: ObjectDetector
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var database: DatabaseReference

    private var lastSpokenObject: String? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // Default to back camera

    companion object {
        private const val TAG = "ObjectDetection"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_object_detection)

        previewView = findViewById(id.previewView)
        detectedText = findViewById(id.detectedObjectText)
        overlayView = findViewById(id.overlayView)
        boundingBoxCanvas = findViewById(id.boundingBoxCanvas)
        switchCameraButton = findViewById(id.switchCameraButton)

        textToSpeech = TextToSpeech(this, this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        database = FirebaseDatabase.getInstance().reference.child("DetectedObjects")

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setupObjectDetector()

        switchCameraButton.setOnClickListener {
            switchCamera()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        detectObjects(image)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun setupObjectDetector() {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()

        objectDetector = ObjectDetector.createFromFileAndOptions(this, "1.tflite", options)
    }

    private fun detectObjects(image: ImageProxy) {
        try {
            val bitmap = image.toBitmap()

            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results = objectDetector.detect(tensorImage)

            if (results.isNotEmpty()) {
                runOnUiThread {
                    val detectedObjects = results.joinToString(", ") {
                        "${it.categories[0].label}: ${String.format("%.2f", it.categories[0].score * 100)}%"
                    }
                    detectedText.text = detectedObjects
                    overlayView.visibility = View.VISIBLE
                    detectedText.visibility = View.VISIBLE

                    if (results[0].categories[0].label != lastSpokenObject) {
                        speakDetectedObject(results[0].categories[0].label)
                        lastSpokenObject = results[0].categories[0].label
                        saveDetectedObjectToFirebase(results[0].categories[0].label)
                    }

                    drawBoundingBoxes(bitmap, results.map { it.boundingBox })
                }
            } else {
                runOnUiThread {
                    boundingBoxCanvas.setImageBitmap(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in object detection: ${e.message}")
        } finally {
            image.close()
        }
    }

    private fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<RectF>) {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        for (box in boxes) {
            canvas.drawRect(box, paint)
        }
        boundingBoxCanvas.setImageBitmap(mutableBitmap)
    }

    private fun saveDetectedObjectToFirebase(objectName: String) {
        database.orderByKey().limitToLast(1).get().addOnSuccessListener { snapshot ->
            val lastId = snapshot.children.lastOrNull()?.child("id")?.getValue(Int::class.java) ?: 0
            val newId = lastId + 1 // Auto-increment ID logic

            val detectedObject = DetectedObjectFirebase(
                id = newId,
                objectName = objectName,
                detectedTime = System.currentTimeMillis()
            )

            database.child(newId.toString()).setValue(detectedObject)
                .addOnSuccessListener {
                    Log.d(TAG, "Object saved to Firebase: $objectName at ${detectedObject.detectedTime}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save object to Firebase: ${e.message}")
                }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get last ID: ${e.message}")
        }
    }


    private fun speakDetectedObject(objectName: String) {
        val message = "Detected $objectName"
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
        } else {
            Log.e(TAG, "Text-to-Speech initialization failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
        cameraExecutor.shutdown()
    }
}
