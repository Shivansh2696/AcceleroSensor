package com.cybersuiteinc.accelerosensor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.cybersuiteinc.accelerosensor.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.IOException

class MainActivity : AppCompatActivity(), SensorEventListener{

    private lateinit var binding : ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var geocoder: Geocoder

    companion object{
        const val SENSOR_NOT_AVAILABLE = "Sensor not available"
        const val UP = "Sensor not available"
        const val DOWN = "Sensor not available"
        const val LEFT = "Sensor not available"
        const val RIGHT = "Sensor not available"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        askLocationPermission()
        geocoder = Geocoder(this)

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize location request
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Update location every 10 seconds (adjust as needed)
            fastestInterval = 2000 // Fastest update interval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }




        if (accelerometer == null) {
            binding.tvSensorStatus.text = "Sensor not available"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val sensorValue = 7.5

            // Here you can implement your logic based on the accelerometer data
            if (y > sensorValue) {
                // Phone is lifted up
                binding.tvSensorStatus.text = "UP"
            } else if (y < - 5.0) {
                // Phone is dropped down
                binding.tvSensorStatus.text = "DOWN"
            } else if (x > sensorValue) {
                // Phone is moved to the right
                binding.tvSensorStatus.text = "LEFT"
            } else if (x < - sensorValue) {
                // Phone is moved to the left
                binding.tvSensorStatus.text = "RIGHT"
            }
        }

        updateLocation()
    }


    private fun updateLocation() {
        if (checkLocationPermission()) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(resultLocation : LocationResult) {
                    resultLocation.lastLocation?.let { location ->
                        val latitude = location.latitude
                        val longitude = location.longitude
                        binding.tvLocationStatus.text = "Latitude: $latitude\nLongitude: $longitude"
                        getStreetAndCity(latitude,longitude)
                    }
                }
            }, null)
        }
    }

    private fun getStreetAndCity(latitude: Double, longitude: Double) {
        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address: Address = addresses[0]
                val cityName: String? = address.locality
                val streetName: String? = address.thoroughfare

                // Update UI with street and city name
                binding.tvAddress.text = "City: $cityName\nStreet: $streetName"
            } else {
                // No address found
                binding.tvAddress.text = "Address not found"
            }
        } catch (e: IOException) {
            // Handle IO exception
            e.printStackTrace()
        }
    }
    private fun checkLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val res: Int = checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private val locationPermissionLauncher = (this).registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
        if (!isGranted){
            Toast.makeText(this,"Location permission required", Toast.LENGTH_LONG).show()
            openSettingsLocation()
        }
    }

    private val locationResultLauncher = (this).registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ _ ->
        val isLocationGranted = checkLocationPermission()
        if(!isLocationGranted){
            showAlertLocation()
        }
    }

    // This function navigates user to setting page where user can give location permission manually
    private fun openSettingsLocation() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("Package", packageName, null)
        intent.data = uri
        locationResultLauncher.launch(intent)
    }

    // This function will ask location permission to user at runtime
    private fun askLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        }
    }

    //    This function show alert dialog to user for Location Permission
    private fun showAlertLocation(){
        val builder = AlertDialog.Builder(this)
            .setTitle("Location permission required")
            .setMessage("Please allow the permission")
            .setPositiveButton("Allow") { _, _ ->
                openSettingsLocation()
            }
            .setNegativeButton("Deny") { _, _ ->
                Toast.makeText(this, "Permission Denied.", Toast.LENGTH_LONG).show()
                finish()
            }
            .create()
        builder.setCancelable(false)
        builder.show()
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