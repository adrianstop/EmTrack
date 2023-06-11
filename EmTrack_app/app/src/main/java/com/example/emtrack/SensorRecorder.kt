package com.example.emtrack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class SensorRecorder (var mContext: Context?) : SensorEventListener {

    private val TAG = "SensorRecorder"
    private val MEAS_WINDOW_SIZE = 50
    val MIN_WINDOWS = 3
    val NUM_SENSORS = 20

    private lateinit var mSensorManager: SensorManager
    private var mSensorAcc: Sensor? = null
    private var mSensorGyro: Sensor? = null
    private var mSensorMag: Sensor? = null
    private var mSensorLinAcc: Sensor? = null
    private var mSensorOrientation: Sensor? = null
    private var accX: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var accY: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var accZ: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var absAcc: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var gyroX: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var gyroY: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var gyroZ: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var absGyro: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var magX: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var magY: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var magZ: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var absMag: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var linAccX: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var linAccY: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var linAccZ: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var absLinAcc: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var orientW: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var orientX: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var orientY: FloatArray = FloatArray(MEAS_WINDOW_SIZE)
    private var orientZ: FloatArray = FloatArray(MEAS_WINDOW_SIZE)

    var data: MutableList<Float> = ArrayList(NUM_SENSORS*MIN_WINDOWS)

    private var accMeasFlag: Boolean = false
    private var gyroMeasFlag: Boolean = false
    private var magMeasFlag: Boolean = false
    private var linAccMeasFlag: Boolean = false
    private var orientMeasFlag: Boolean = false
    var dataReadyFlag: Boolean = false

    private var globalMeasCount: Int = 0
    var recWindowCount: Int = 0
        set(value) {
            field = value
            recListener?.onRecWindowsChanged(value)
        }

    var recListener: RecorderListener? = null

    private lateinit var logFile: File

    public fun initSensors() {
        mSensorManager = mContext!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensorLinAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (mSensorAcc != null) {
            mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Accelerometer initialized")
        }
        if (mSensorGyro != null) {
            mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Gyroscope initialized")
        }
        if (mSensorMag != null) {
            mSensorManager.registerListener(this, mSensorMag, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Magnetometer initialized")
        }
        if (mSensorLinAcc != null) {
            mSensorManager.registerListener(this, mSensorLinAcc, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Linear Accelerometer initialized")
        }
        if (mSensorOrientation != null) {
            mSensorManager.registerListener(this, mSensorOrientation, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Orientation initialized")
        }

        Log.d(TAG, "Sensors initialized")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(globalMeasCount < MEAS_WINDOW_SIZE - 1){
            // Reset flags when all sensors have been measured
            if(accMeasFlag && gyroMeasFlag && magMeasFlag && linAccMeasFlag && orientMeasFlag){
                accMeasFlag = false
                gyroMeasFlag = false
                magMeasFlag = false
                linAccMeasFlag = false
                orientMeasFlag = false
                globalMeasCount++
                // Log.d(TAG, "Global meas count: $globalMeasCount")
            }
            val i = globalMeasCount
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    if(!accMeasFlag){
                        accX[i] = event.values[0]
                        accY[i] = event.values[1]
                        accZ[i] = event.values[2]
                        absAcc[i] = sqrt(accX[i].pow(2) + accY[i].pow(2) + accZ[i].pow(2))
                        accMeasFlag = true
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if(!gyroMeasFlag){
                        gyroX[i] = event.values[0]
                        gyroY[i] = event.values[1]
                        gyroZ[i] = event.values[2]
                        absGyro[i] = sqrt(gyroX[i].pow(2) + gyroY[i].pow(2) + gyroZ[i].pow(2))
                        gyroMeasFlag = true
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    if(!magMeasFlag){
                        magX[i] = event.values[0]
                        magY[i] = event.values[1]
                        magZ[i] = event.values[2]
                        absMag[i] = sqrt(magX[i].pow(2) + magY[i].pow(2) + magZ[i].pow(2))
                        magMeasFlag = true
                    }
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    if(!linAccMeasFlag){
                        linAccX[i] = event.values[0]
                        linAccY[i] = event.values[1]
                        linAccZ[i] = event.values[2]
                        absLinAcc[i] = sqrt(linAccX[i].pow(2) + linAccY[i].pow(2) + linAccZ[i].pow(2))
                        linAccMeasFlag = true
                    }
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    if(!orientMeasFlag){
                        orientW[i] = event.values[3]
                        orientX[i] = event.values[0]
                        orientY[i] = event.values[1]
                        orientZ[i] = event.values[2]
                        orientMeasFlag = true
                    }
                }
                else -> {}
            }
        }
        else{
            // Window complete -> add to data
            // Drop last values
            shiftData(NUM_SENSORS)

            // Push in elements in reverse order to match the order in which they are read
            data.add(0, (absLinAcc.sum() / absLinAcc.size))
            data.add(0, (absMag.sum() / absMag.size))
            data.add(0, (absGyro.sum() / absGyro.size))
            data.add(0, (absAcc.sum() / absAcc.size))
            data.add(0, (linAccZ.sum() / linAccZ.size))
            data.add(0, (linAccY.sum() / linAccY.size))
            data.add(0, (linAccX.sum() / linAccX.size))
            data.add(0, (orientZ.sum() / orientZ.size))
            data.add(0, (orientY.sum() / orientY.size))
            data.add(0, (orientX.sum() / orientX.size))
            data.add(0, (orientW.sum() / orientW.size))
            data.add(0, (magZ.sum() / magZ.size))
            data.add(0, (magY.sum() / magY.size))
            data.add(0, (magX.sum() / magX.size))
            data.add(0, (gyroZ.sum() / gyroZ.size))
            data.add(0, (gyroY.sum() / gyroY.size))
            data.add(0, (gyroX.sum() / gyroX.size))
            data.add(0, (accZ.sum() / accZ.size))
            data.add(0, (accY.sum() / accY.size))
            data.add(0, (accX.sum() / accX.size))

            Log.d(TAG, "New data appended! data: $data")

            if(recWindowCount >= MIN_WINDOWS){
                data = data.slice(0 until NUM_SENSORS*MIN_WINDOWS).toMutableList()
                dataReadyFlag = true
            }
            recWindowCount++
            globalMeasCount = 0
        }
    }
    private fun shiftData(numPlaces: Int) {
        for(i in 0 until data.size - numPlaces){
            data[i + numPlaces] = data[i]
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d(TAG, "Accuracy changed")
    }
}
interface RecorderListener {
    fun onRecWindowsChanged(value: Int)
}