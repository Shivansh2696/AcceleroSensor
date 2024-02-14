package com.cybersuiteinc.accelerosensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cybersuiteinc.accelerosensor.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener{

    private lateinit var binding : ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastOrientation: String = "UNKNOWN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val deltaZ = event.values[2] - SensorManager.GRAVITY_EARTH

            if (abs(deltaZ) > 2) {
                if (deltaZ > 0) {
                    if (lastOrientation != "UP") {
                        lastOrientation = "UP"
                        "Dropped".also { binding.tvStatus.text = it }
                    }
                } else {
                    if (lastOrientation != "DOWN") {
                        lastOrientation = "DOWN"
                        "Getting Up".also { binding.tvStatus.text = it }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener
        sensorManager.unregisterListener(this)
    }
}