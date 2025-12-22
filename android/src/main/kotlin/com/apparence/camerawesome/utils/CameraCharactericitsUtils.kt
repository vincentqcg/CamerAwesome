package com.apparence.camerawesome.utils

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import com.apparence.camerawesome.cameraX.PigeonSensorPosition
import com.apparence.camerawesome.cameraX.PigeonSensorType
import kotlin.math.max
import kotlin.math.min

// 35mm is 135 film format, a standard in which focal lengths are usually measured
val Size35mm = Size(36, 24)

/**
 * Convert a given array of focal lengths to the corresponding TypeScript union type name.
 *
 * Possible values for single cameras:
 * * `"wide-angle-camera"`
 * * `"ultra-wide-angle-camera"`
 * * `"telephoto-camera"`
 *
 * Sources for the focal length categories:
 * * [Telephoto Lens (wikipedia)](https://en.wikipedia.org/wiki/Telephoto_lens)
 * * [Normal Lens (wikipedia)](https://en.wikipedia.org/wiki/Normal_lens)
 * * [Wide-Angle Lens (wikipedia)](https://en.wikipedia.org/wiki/Wide-angle_lens)
 * * [Ultra-Wide-Angle Lens (wikipedia)](https://en.wikipedia.org/wiki/Ultra_wide_angle_lens)
 */
@ExperimentalCamera2Interop
fun Camera2CameraInfo.getSensorType(): PigeonSensorType {
    val focalLengths =
        this.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!
    val sensorSize =
        this.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!

    return classifySensorType(focalLengths, sensorSize)
}

@ExperimentalCamera2Interop
fun Camera2CameraInfo.getPigeonPosition(): PigeonSensorPosition {
    val facing = this.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)!!
    return if (facing == LENS_FACING_BACK)
        PigeonSensorPosition.BACK
    else
        PigeonSensorPosition.FRONT
}


val Size.bigger: Int
    get() = max(this.width, this.height)
val Size.smaller: Int
    get() = min(this.width, this.height)

val SizeF.bigger: Float
    get() = max(this.width, this.height)
val SizeF.smaller: Float
    get() = min(this.width, this.height)

/**
 * Classify sensor type based on focal length and sensor size from Camera2 CameraCharacteristics.
 *
 * The classification is performed by converting the provided focal lengths to their 35mm-equivalent
 * values using a crop factor derived from the physical sensor size:
 *
 * `cropFactor = Size35mm.bigger / sensorSize.bigger`
 *
 * where `Size35mm` is 36x24mm (the 135 film / "full-frame" standard) and `sensorSize` is the
 * physical size reported by the camera. Each entry in `focalLengths` (in millimeters) is
 * multiplied by this crop factor to obtain a 35mm-equivalent focal length. The sensor is then
 * categorized as:
 *
 * * Ultra-wide: any 35mm-equivalent focal length **< 20mm**
 * * Wide-angle: any 35mm-equivalent focal length in the range **20mm–35mm** (inclusive)
 * * Telephoto: any 35mm-equivalent focal length **> 35mm**
 *
 * If none of the available focal lengths fall into these ranges, or if focal length or sensor
 * size information is missing, the sensor type is reported as [PigeonSensorType.UNKNOWN].
 *
 * @param focalLengths Array of available focal lengths in millimeters from camera characteristics
 * @param sensorSize Physical sensor size from camera characteristics
 * @return Classified sensor type (TELEPHOTO, WIDEANGLE, ULTRAWIDEANGLE, or UNKNOWN)
 */
fun classifySensorType(
    focalLengths: FloatArray?,
    sensorSize: SizeF?
): PigeonSensorType {
    if (focalLengths == null || focalLengths.isEmpty() || sensorSize == null) {
        return PigeonSensorType.UNKNOWN
    }

    // To get valid focal length standards we have to upscale to the 35mm measurement (film standard)
    val cropFactor = Size35mm.bigger / sensorSize.bigger

    val containsTelephoto =
        focalLengths.any { l -> (l * cropFactor) > 35 } // TODO: Telephoto lenses are > 85mm, but we don't have anything between that range..
    val containsWideAngle =
        focalLengths.any { l -> (l * cropFactor) >= 20 && (l * cropFactor) <= 35 }
    val containsUltraWideAngle = focalLengths.any { l -> (l * cropFactor) < 20 }

    if (containsTelephoto)
        return PigeonSensorType.TELEPHOTO
    if (containsWideAngle)
        return PigeonSensorType.WIDEANGLE
    if (containsUltraWideAngle)
        return PigeonSensorType.ULTRAWIDEANGLE
    return PigeonSensorType.UNKNOWN
}