package com.example.emtrack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class SensorRecorder (var mContext: Context?) : SensorEventListener {

    private val TAG = "SensorRecorder"
    private val MEAS_WINDOW_SIZE = 50
    private val MIN_WINDOWS = 3
    private val NUM_SENSORS = 21

    private lateinit var mSensorManager: SensorManager
    private var mSensorAcc: Sensor? = null
    private var mSensorGyro: Sensor? = null
    private var mSensorMag: Sensor? = null
    private var mSensorLinAcc: Sensor? = null
    private var mSensorOrientation: Sensor? = null
    private var mSensorPressure: Sensor? = null
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
    private var pressure: FloatArray = FloatArray(MEAS_WINDOW_SIZE)

    public var data: FloatArray = FloatArray(NUM_SENSORS * MIN_WINDOWS)

    private var accMeasFlag: Boolean = false
    private var gyroMeasFlag: Boolean = false
    private var magMeasFlag: Boolean = false
    private var linAccMeasFlag: Boolean = false
    private var orientMeasFlag: Boolean = false
    private var pressureMeasFlag: Boolean = false

    private var globalMeasCount: Int = 0
    private var recWindowCount: Int = 0

    private lateinit var logFile: File

    public fun initSensors() {
        mSensorManager = mContext!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensorLinAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mSensorPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (mSensorAcc != null) {
            mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorGyro != null) {
            mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorMag != null) {
            mSensorManager.registerListener(this, mSensorMag, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorLinAcc != null) {
            mSensorManager.registerListener(this, mSensorLinAcc, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorOrientation != null) {
            mSensorManager.registerListener(this, mSensorOrientation, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorPressure != null) {
            mSensorManager.registerListener(this, mSensorPressure, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(globalMeasCount < MEAS_WINDOW_SIZE - 1){
            // Reset flags when all sensors have been measured
            if(accMeasFlag && gyroMeasFlag && magMeasFlag && linAccMeasFlag && orientMeasFlag && pressureMeasFlag){
                accMeasFlag = false
                gyroMeasFlag = false
                magMeasFlag = false
                linAccMeasFlag = false
                orientMeasFlag = false
                pressureMeasFlag = false
                globalMeasCount++
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
                Sensor.TYPE_PRESSURE -> {
                    if(!pressureMeasFlag){
                        pressure[i] = event.values[0]
                        pressureMeasFlag = true
                    }
                }
                else -> {}
            }
        }
        else{
            // Window complete -> write to file
            val meanAccX = (accX.sum()/accX.size)
            val meanAccY = (accY.sum()/accY.size)
            val meanAccZ = (accZ.sum()/accZ.size)
            val meanGyroX = (gyroX.sum()/gyroX.size)
            val meanGyroY = (gyroY.sum()/gyroY.size)
            val meanGyroZ = (gyroZ.sum()/gyroZ.size)
            val meanMagX = (magX.sum()/magX.size)
            val meanMagY = (magY.sum()/magY.size)
            val meanMagZ = (magZ.sum()/magZ.size)
            val meanOrientW = (orientW.sum()/orientW.size)
            val meanOrientX = (orientX.sum()/orientX.size)
            val meanOrientY = (orientY.sum()/orientY.size)
            val meanOrientZ = (orientZ.sum()/orientZ.size)
            val meanPressure = (pressure.sum()/pressure.size)

            if(recWindowCount > MIN_WINDOWS){

            }
            recWindowCount++
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}