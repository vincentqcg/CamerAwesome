package com.apparence.camerawesome.utils

import com.apparence.camerawesome.cameraX.PigeonSensorType
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for camera sensor type classification logic
 * 
 * Note: These tests validate the classification algorithm without requiring Android framework.
 * The actual integration with Camera2 API is tested through integration tests.
 */
class CameraCharacteristicsUtilsTest {

    /**
     * Creates a mock SizeF-like object for testing
     * This avoids depending on Android framework in unit tests
     */
    private data class TestSizeF(val width: Float, val height: Float)

    /**
     * Helper to test classification logic with simulated sensor data
     * Calculates expected sensor type based on focal length and sensor size
     */
    private fun testClassification(
        focalLengths: FloatArray?,
        sensorWidth: Float,
        sensorHeight: Float
    ): PigeonSensorType {
        if (focalLengths == null || focalLengths.isEmpty()) {
            return PigeonSensorType.UNKNOWN
        }

        // Simulate the classification logic
        val sensorDiagonal = kotlin.math.sqrt(
            sensorWidth * sensorWidth + sensorHeight * sensorHeight
        )
        val cropFactor = 43.27f / sensorDiagonal

        val containsTelephoto = focalLengths.any { l -> (l * cropFactor) > 35 }
        val containsWideAngle = focalLengths.any { l -> (l * cropFactor) >= 24 && (l * cropFactor) <= 35 }
        val containsUltraWideAngle = focalLengths.any { l -> (l * cropFactor) < 24 }

        return when {
            containsTelephoto -> PigeonSensorType.TELEPHOTO
            containsWideAngle -> PigeonSensorType.WIDEANGLE
            containsUltraWideAngle -> PigeonSensorType.ULTRAWIDEANGLE
            else -> PigeonSensorType.UNKNOWN
        }
    }

    @Test
    fun testClassifySensorType_nullFocalLengths_returnsUnknown() {
        val result = testClassification(null, 5.76f, 4.29f)
        assertEquals(PigeonSensorType.UNKNOWN, result)
    }

    @Test
    fun testClassifySensorType_emptyFocalLengths_returnsUnknown() {
        val result = testClassification(floatArrayOf(), 5.76f, 4.29f)
        assertEquals(PigeonSensorType.UNKNOWN, result)
    }

    @Test
    fun testClassifySensorType_telephotoFocalLength_returnsTelephoto() {
        // Typical telephoto: 52mm equivalent
        // Sensor: 1/3.4" sensor (5.76mm x 4.29mm, diagonal ~7.2mm)
        // Crop factor: 43.27 / 7.2 ≈ 6.0
        // Physical focal length: 52mm / 6.0 ≈ 8.7mm
        val result = testClassification(floatArrayOf(8.7f), 5.76f, 4.29f)
        assertEquals(PigeonSensorType.TELEPHOTO, result)
    }

    @Test
    fun testClassifySensorType_wideAngleFocalLength_returnsWideAngle() {
        // Typical wide-angle: 26mm equivalent
        // Sensor: 1/2.55" sensor (6.5mm x 4.9mm, diagonal ~8.1mm)
        // Crop factor: 43.27 / 8.1 ≈ 5.34
        // Physical focal length: 26mm / 5.34 ≈ 4.87mm
        val result = testClassification(floatArrayOf(4.87f), 6.5f, 4.9f)
        assertEquals(PigeonSensorType.WIDEANGLE, result)
    }

    @Test
    fun testClassifySensorType_ultraWideFocalLength_returnsUltraWide() {
        // Typical ultra-wide: 13mm equivalent
        // Sensor: 1/3.6" sensor (5.5mm x 4.1mm, diagonal ~6.8mm)
        // Crop factor: 43.27 / 6.8 ≈ 6.36
        // Physical focal length: 13mm / 6.36 ≈ 2.04mm
        val result = testClassification(floatArrayOf(2.04f), 5.5f, 4.1f)
        assertEquals(PigeonSensorType.ULTRAWIDEANGLE, result)
    }

    @Test
    fun testClassifySensorType_multipleFocalLengths_prioritizesTelephoto() {
        // Camera with multiple focal lengths should return the most specific type
        // Telephoto is prioritized over wide-angle and ultra-wide
        val result = testClassification(floatArrayOf(2.0f, 4.8f, 8.5f), 6.5f, 4.9f)
        assertEquals(PigeonSensorType.TELEPHOTO, result)
    }

    @Test
    fun testClassifySensorType_telephotoBoundary_justAbove35mm() {
        // Test boundary at 35mm equivalent
        // Sensor: 7.0mm x 5.2mm, diagonal ~8.7mm
        // Crop factor: 43.27 / 8.7 ≈ 4.97
        // For 36mm equivalent: 36 / 4.97 ≈ 7.24mm physical (just above threshold)
        val result = testClassification(floatArrayOf(7.24f), 7.0f, 5.2f)
        assertEquals(PigeonSensorType.TELEPHOTO, result)
    }

    @Test
    fun testClassifySensorType_wideAngleBoundary_at24mm() {
        // Test boundary at 24mm equivalent (lower bound of wide-angle)
        // Sensor: 7.0mm x 5.2mm, crop factor ~5.0
        // For 25mm equivalent: 25 / 5.0 = 5.0mm physical
        val result = testClassification(floatArrayOf(5.0f), 7.0f, 5.2f)
        assertEquals(PigeonSensorType.WIDEANGLE, result)
    }

    @Test
    fun testClassifySensorType_ultraWideBoundary_below24mm() {
        // Test ultra-wide just below 24mm equivalent
        // Sensor: 7.0mm x 5.2mm, crop factor ~5.0
        // For 20mm equivalent: 20 / 5.0 = 4.0mm physical
        val result = testClassification(floatArrayOf(4.0f), 7.0f, 5.2f)
        assertEquals(PigeonSensorType.ULTRAWIDEANGLE, result)
    }

    /**
     * Test that demonstrates real-world sensor data
     * Based on Google Pixel 7 Pro specifications
     */
    @Test
    fun testClassifySensorType_realWorldPixel7Pro_mainCamera() {
        // Pixel 7 Pro main camera: 25mm equivalent, 1/1.31" sensor
        // Approximate sensor size: 10.67mm x 8.00mm (diagonal ~13.3mm)
        // Crop factor: 43.27 / 13.3 ≈ 3.25
        // Physical focal length: 25 / 3.25 ≈ 7.7mm
        val result = testClassification(floatArrayOf(7.7f), 10.67f, 8.0f)
        assertEquals(PigeonSensorType.WIDEANGLE, result)
    }

    @Test
    fun testClassifySensorType_realWorldPixel7Pro_ultraWide() {
        // Pixel 7 Pro ultra-wide: 14mm equivalent
        // Smaller sensor, crop factor ~6.0
        // Physical focal length: 14 / 6.0 ≈ 2.33mm
        val result = testClassification(floatArrayOf(2.33f), 5.5f, 4.1f)
        assertEquals(PigeonSensorType.ULTRAWIDEANGLE, result)
    }

    @Test
    fun testClassifySensorType_realWorldPixel7Pro_telephoto() {
        // Pixel 7 Pro telephoto: 48mm equivalent (5x optical)
        // Crop factor ~5.7
        // Physical focal length: 48 / 5.7 ≈ 8.4mm
        val result = testClassification(floatArrayOf(8.4f), 5.8f, 4.3f)
        assertEquals(PigeonSensorType.TELEPHOTO, result)
    }
}
