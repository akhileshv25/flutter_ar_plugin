package com.example.flutter_ar_plugin

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.sqrt

fun deserializeMatrix4(transform: ArrayList<Double>): Triple<Vector3, Vector3, Quaternion> {
    if (transform.size < 16) {
        throw IllegalArgumentException("Transformation matrix must have at least 16 elements, but has ${transform.size}")
    }

    val scale = Vector3()
    val position = Vector3()
    val rotation: Quaternion

    // Compute scale safely
    scale.x = sqrt(transform[0] * transform[0] + transform[1] * transform[1] + transform[2] * transform[2]).toFloat()
    scale.y = sqrt(transform[4] * transform[4] + transform[5] * transform[5] + transform[6] * transform[6]).toFloat()
    scale.z = sqrt(transform[8] * transform[8] + transform[9] * transform[9] + transform[10] * transform[10]).toFloat()

    // Prevent division by zero
    if (scale.x == 0f || scale.y == 0f || scale.z == 0f) {
        throw IllegalArgumentException("Invalid scale: $scale (division by zero detected)")
    }

    // Extract translation
    position.x = transform[12].toFloat()
    position.y = transform[13].toFloat()
    position.z = transform[14].toFloat()

    // Normalize rotation matrix
    val rowWiseMatrix = floatArrayOf(
        (transform[0] / scale.x).toFloat(), (transform[4] / scale.y).toFloat(), (transform[8] / scale.z).toFloat(),
        (transform[1] / scale.x).toFloat(), (transform[5] / scale.y).toFloat(), (transform[9] / scale.z).toFloat(),
        (transform[2] / scale.x).toFloat(), (transform[6] / scale.y).toFloat(), (transform[10] / scale.z).toFloat()
    )

    // Compute quaternion from rotation matrix
    val trace = rowWiseMatrix[0] + rowWiseMatrix[4] + rowWiseMatrix[8]
    val q = FloatArray(4)

    if (trace > 0) {
        val s = sqrt(trace + 1.0).toFloat() * 2
        q[3] = 0.25f * s
        q[0] = (rowWiseMatrix[7] - rowWiseMatrix[5]) / s
        q[1] = (rowWiseMatrix[2] - rowWiseMatrix[6]) / s
        q[2] = (rowWiseMatrix[3] - rowWiseMatrix[1]) / s
    } else if ((rowWiseMatrix[0] > rowWiseMatrix[4]) && (rowWiseMatrix[0] > rowWiseMatrix[8])) {
        val s = sqrt(1.0 + rowWiseMatrix[0] - rowWiseMatrix[4] - rowWiseMatrix[8]).toFloat() * 2
        q[3] = (rowWiseMatrix[7] - rowWiseMatrix[5]) / s
        q[0] = 0.25f * s
        q[1] = (rowWiseMatrix[1] + rowWiseMatrix[3]) / s
        q[2] = (rowWiseMatrix[2] + rowWiseMatrix[6]) / s
    } else if (rowWiseMatrix[4] > rowWiseMatrix[8]) {
        val s = sqrt(1.0 + rowWiseMatrix[4] - rowWiseMatrix[0] - rowWiseMatrix[8]).toFloat() * 2
        q[3] = (rowWiseMatrix[2] - rowWiseMatrix[6]) / s
        q[0] = (rowWiseMatrix[1] + rowWiseMatrix[3]) / s
        q[1] = 0.25f * s
        q[2] = (rowWiseMatrix[5] + rowWiseMatrix[7]) / s
    } else {
        val s = sqrt(1.0 + rowWiseMatrix[8] - rowWiseMatrix[0] - rowWiseMatrix[4]).toFloat() * 2
        q[3] = (rowWiseMatrix[3] - rowWiseMatrix[1]) / s
        q[0] = (rowWiseMatrix[2] + rowWiseMatrix[6]) / s
        q[1] = (rowWiseMatrix[5] + rowWiseMatrix[7]) / s
        q[2] = 0.25f * s
    }

    val inputRotation = Quaternion(q[0], q[1], q[2], q[3])

    // **Fix correction quaternions (convert 180° to radians: Math.toRadians(180.0))**
    val correction_z = Quaternion.axisAngle(Vector3(0f, 0f, 1f), Math.toRadians(180.0).toFloat())
    val correction_y = Quaternion.axisAngle(Vector3(0f, 1f, 0f), Math.toRadians(180.0).toFloat())

    // Apply corrections
    rotation = Quaternion.multiply(Quaternion.multiply(inputRotation, correction_y), correction_z)

    return Triple(scale, position, rotation)
}
