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
import android.location.*
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.firstapp.utils.AccelerometerData
import com.example.firstapp.utils.FirebaseUtils
import java.lang.Float.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


@SuppressLint("ObsoleteSdkInt")
class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private var mLastX = 0f
    private var mLastY = 0f
    private var mLastZ = 0f
    private var mInitialized = false
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var getLocationBtn: Button? = null
    private var countTime: TextView? = null
    private var locationText: TextView? = null
    private var locationManager: LocationManager? = null
    private var permissionsToRequest: ArrayList<String>? = null
    private val permissionsRejected: ArrayList<String> = ArrayList()
    private val permissions = ArrayList<String>()

    private var xD = 0.0F
    private var yD = 0.0F
    private var zD = 0.0F

    private var geocoder: Geocoder? = null
    private var addresses: List<Address>? = null
    private var metaDataState = false
    private var accelerometerData = AccelerometerData()

    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var landMark: String = ""

    private var counter = 0
    private var toggleMetaData = false
    private var toggleMainHandler = true
    private lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissions.add(permission.ACCESS_FINE_LOCATION)
        permissions.add(permission.ACCESS_COARSE_LOCATION)
        permissionsToRequest = findUnAskedPermissions(permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest!!.size > 0) requestPermissions(
                permissionsToRequest!!.toTypedArray(),
                ALL_PERMISSIONS_RESULT
            )
        }
        getLocationBtn = findViewById<View>(R.id.getLocationBtn) as Button
        locationText = findViewById<View>(R.id.locationText) as TextView
        countTime = findViewById<View>(R.id.countTime) as TextView

        geocoder = Geocoder(this,Locale.getDefault())

        getLocationBtn!!.setOnClickListener {
            if(toggleMetaData) {
                countDownTimer.cancel()
                countTime!!.text = getString(R.string.time_placeholder)
                toggleMetaData = false
                toggleMainHandler = false
                getLocationBtn!!.text = getString(R.string.start)
                getLocationBtn!!.backgroundTintList = ContextCompat.getColorStateList(this,R.color.git_green)
            } else {
                toggleMetaData = true
                toggleMainHandler = true
                getLocationBtn!!.text = getString(R.string.stop)
                getLocationBtn!!.backgroundTintList = ContextCompat.getColorStateList(this,R.color.red_follow)
                location
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post(object : Runnable {
                    override fun run() {
                        if(toggleMainHandler) {
                            mainHandler.postDelayed(this, 100)
                            val s = SimpleDateFormat(getString(R.string.date_pattern))
                            accelerometerData.timestamp = s.format(Date()).toString()
                            accelerometerData.lat = latitude
                            accelerometerData.long = longitude
                            accelerometerData.xAxis = xD
                            accelerometerData.yAxis = yD
                            accelerometerData.zAxis = zD
                            if(landMark != "") {
                                Log.d("AccData","${accelerometerData.xAxis} , ${accelerometerData.yAxis} , ${accelerometerData.zAxis}")
                                uploadAccelerometerData(landMark, accelerometerData)
                            }
                        }
                    }
                })
                startTimeCounter()
            }
        }

        mInitialized = false
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startTimeCounter() {
        countDownTimer = object : CountDownTimer(INF_TIME,INTERVAL_SEC) {
            @SuppressLint("DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                countTime?.visibility = View.VISIBLE
                val ms = (INF_TIME - millisUntilFinished)
                val time = java.lang.String.format(
                    "%02d min %02d sec",
                    TimeUnit.MILLISECONDS.toMinutes(ms) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(ms)),
                    TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(ms))
                )
                countTime?.text = time
                counter++
            }
            override fun onFinish() {
                "Session Stopped".also { countTime?.text = it }
            }
        }.start()
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
            if(deltaX != 0.0F || deltaY != 0.0F || deltaZ != 0.0F) {
                xD = deltaX
                yD = deltaY
                zD = deltaZ
            }
            tvX.text = deltaX.toString()
            tvY.text = deltaY.toString()
            tvZ.text = deltaZ.toString()
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

    private val location: Unit
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
        latitude = location.latitude
        longitude = location.longitude
        addresses = geocoder?.getFromLocation(location.latitude, location.longitude, 1)
        Log.d("Address", addresses?.size.toString() + "\n" + addresses.toString())
        landMark = (addresses?.get(0)?.featureName).toString()
        val locality = (addresses?.get(0)?.locality).toString()
        val postalCode = (addresses?.get(0)?.postalCode).toString()
        locationText!!.text = buildString {
        append("Current Location: ")
        append(location.latitude)
        append(", ")
        append(location.longitude)
        append("\nLandmark: ")
        append(landMark)
        }
        if(landMark.isNotEmpty() && !metaDataState) {
            metaDataState = true
            val dataMap  = hashMapOf(
                "landMarkKey" to landMark,
                "locality" to locality,
                "postalCode" to postalCode
            )
            uploadRoadMetaData(dataMap)
        }
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

    @Deprecated("Deprecated in Java")
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
        if (canMakeStores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkSelfPermission((permission as String)) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    private fun canMakeStores(): Boolean {
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
                    if (shouldShowRequestPermissionRationale(permissionsRejected[0])) {
                        showMessageOKCancel { _: DialogInterface?, _: Int ->
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

    private fun uploadRoadMetaData(dataMap: HashMap<String, String>) {
        FirebaseUtils().db.collection("landMarks").document(dataMap["landMarkKey"]!!)
            .set(dataMap)
            .addOnSuccessListener {
                Log.d("DocID", "Added document with ID ${dataMap["landMarkKey"]!!}")
            }
            .addOnFailureListener { exception ->
                Log.w("DocError", "Error adding document $exception")
            }
    }

    private fun uploadAccelerometerData(landMark: String,accelerometerData: AccelerometerData) {
        FirebaseUtils().db.collection("landMarks").document(landMark).collection("accelerometerData")
            .document()
            .set(accelerometerData)
            .addOnSuccessListener {
                Log.d("DocID", "Added document with ID $landMark")
            }
            .addOnFailureListener { exception ->
                Log.w("DocError", "Error adding document $exception")
            }
    }

    companion object {
        private const val ALL_PERMISSIONS_RESULT = 101
        private const val NOISE = 2.0.toFloat()
        private const val INF_TIME = 1000000000000000000L
        private const val INTERVAL_SEC = 1000L
    }
}