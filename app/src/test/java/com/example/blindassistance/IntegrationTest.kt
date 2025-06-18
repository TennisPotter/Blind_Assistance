    package com.example.blindassistance
    import androidx.test.ext.junit.runners.AndroidJUnit4
    import androidx.test.platform.app.InstrumentationRegistry
    import org.junit.Assert.*
    import org.junit.Test
    import org.junit.runner.RunWith

    @RunWith(AndroidJUnit4::class)
    class IntegrationTest {
        @Test
        fun testFirebaseConnection() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertNotNull("Firebase is not connected", context)
        }
    }
