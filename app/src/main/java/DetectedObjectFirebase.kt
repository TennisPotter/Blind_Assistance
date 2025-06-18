package com.example.blindassistance.data

data class DetectedObjectFirebase(
    val id: Int = 0,  // Auto-increment (Handled in Firebase)
    val objectName: String? = null,
    val detectedTime: Long = System.currentTimeMillis() // Store timestamp
)
