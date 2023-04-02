package com.example.firstapp

import android.Manifest.permission
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

@SuppressLint("ObsoleteSdkInt")
class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private var mLastX = 0f
    private var mLastY = 0f
    private var mLastZ = 0f
    private var mInitialized = false
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private val NOISE = 2.0.toFloat()
    private var getLocationBtn: Button? = null
    private var locationText: TextView? = null
    private var locationManager: LocationManager? = null
    private var permissionsToRequest: ArrayList<String>? = null
    private val permissionsRejected: ArrayList<String> = ArrayList<String>()
    private val permissions = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissions.add(permission.ACCESS_FINE_LOCATION)
        permissions.add(permission.ACCESS_COARSE_LOCATION)
        permissionsToRequest = findUnAskedPermissions(permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest!!.size > 0) requestPermissions(
                permissionsToRequest!!.toTypedArray<String>(),
                ALL_PERMISSIONS_RESULT
            )
        }
        getLocationBtn = findViewById<View>(R.id.getLocationBtn) as Button
        locationText = findViewById<View>(R.id.locationText) as TextView
        getLocationBtn!!.setOnClickListener { v: View? -> location }
        mInitialized = false
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val tvX = findViewById<TextView>(R.id.x_axis)
        val tvY = findViewById<TextView>(R.id.y_axis)
        val tvZ = findViewById<TextView>(R.id.z_axis)
        val iv = findViewById<ImageView>(R.id.image)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        if (!mInitialized) {
            mLastX = x
            mLastY = y
            mLastZ = z
            tvX.text = "0.0"
            tvY.text = "0.0"
            tvZ.text = "0.0"
            mInitialized = true
        } else {
            var deltaX = abs(mLastX - x)
            var deltaY = abs(mLastY - y)
            var deltaZ = abs(mLastZ - z)
            if (deltaX < NOISE) deltaX = 0.0.toFloat()
            if (deltaY < NOISE) deltaY = 0.0.toFloat()
            if (deltaZ < NOISE) deltaZ = 0.0.toFloat()
            mLastX = x
            mLastY = y
            mLastZ = z
            tvX.text = java.lang.Float.toString(deltaX)
            tvY.text = java.lang.Float.toString(deltaY)
            tvZ.text = java.lang.Float.toString(deltaZ)
            iv.visibility = View.VISIBLE
            if (deltaX > deltaY) {
                iv.setImageResource(R.drawable.horizontal)
            } else if (deltaY > deltaX) {
                iv.setImageResource(R.drawable.vertical)
            } else {
                iv.visibility = View.INVISIBLE
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
    val location: Unit
        get() {
            try {
                locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                locationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    (this as LocationListener)
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

    override fun onLocationChanged(location: Location) {
        locationText!!.text = "Current Location: " + location.latitude + ", " + location.longitude
        Toast.makeText(
            this,
            "Current Location: " + location.latitude + ", " + location.longitude,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(this@MainActivity, "Please Enable GPS and Internet", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    private fun findUnAskedPermissions(wanted: ArrayList<String>): ArrayList<String> {
        val result = ArrayList<String>()
        for (perm in wanted) {
            if (!hasPermission(perm)) {
                result.add(perm)
            }
        }
        return result
    }

    private fun hasPermission(permission: Any): Boolean {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkSelfPermission((permission as String)) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    private fun canMakeSmores(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (perms in permissionsToRequest!!) {
                if (!hasPermission(perms)) {
                    permissionsRejected.add(perms)
                }
            }
            if (permissionsRejected.size > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale((permissionsRejected[0] as String))) {
                        showMessageOKCancel { dialog: DialogInterface?, which: Int ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(
                                    (permissionsRejected.toTypedArray() as Array<String?>),
                                    ALL_PERMISSIONS_RESULT
                                )
                            }
                        }
                        return
                    }
                }
            }
        }
    }

    private fun showMessageOKCancel(okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this@MainActivity)
            .setMessage("These permissions are mandatory for the application. Please allow access.")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val ALL_PERMISSIONS_RESULT = 101
    }
}