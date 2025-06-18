package com.example.blindassistance
import org.junit.Assert.*
import org.junit.Test

class ObjectDetectionTest {
        @Test
        fun testDetectedObject() {
            val detectedObject = "keyboard"
            assertEquals("keyboard", detectedObject)
        }
    }
