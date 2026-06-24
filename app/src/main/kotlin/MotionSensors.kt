package com.example.saccadacusandroid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Records device-orientation / motion sensors (prompt 007): rotation-vector, gyroscope,
 * accelerometer. Each sample keeps its own SensorEvent.timestamp (nanos) domain and is
 * stored separately — no fusing with the camera signals.
 */
class MotionSensors(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensors = listOfNotNull(
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
    )

    @Volatile
    var active = false
        private set

    fun start() {
        for (sensor in sensors) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        active = sensors.isNotEmpty()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        active = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val name = when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> "rotation_vector"
            Sensor.TYPE_GYROSCOPE -> "gyroscope"
            Sensor.TYPE_ACCELEROMETER -> "accelerometer"
            else -> return
        }
        val v = event.values
        SessionRecorder.addSensorSample(
            name,
            event.timestamp,
            v.getOrElse(0) { 0f },
            v.getOrElse(1) { 0f },
            v.getOrElse(2) { 0f },
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
