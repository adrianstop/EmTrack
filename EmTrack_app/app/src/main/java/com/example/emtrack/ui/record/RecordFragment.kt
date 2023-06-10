package com.example.emtrack.ui.record

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.emtrack.databinding.FragmentRecordBinding
import com.example.emtrack.R
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.paramsen.noise.Noise
import kotlin.math.pow
import kotlin.math.sqrt

class RecordFragment : Fragment(), SensorEventListener, AdapterView.OnItemSelectedListener{

    private var _binding: FragmentRecordBinding? = null
    private lateinit var mSensorManager: SensorManager
    private var mSensorAcc: Sensor? = null
    private var mSensorGyro: Sensor? = null
    private var mSensorMag: Sensor? = null
    private var mSensorLinAcc: Sensor? = null
    private var mSensorOrientation: Sensor? = null
    private var mSensorTemp: Sensor? = null
    private var accArr: FloatArray = FloatArray(512)
    private var gyroArr: FloatArray = FloatArray(512)
    private var magArr: FloatArray = FloatArray(512)
    private var fftAccArr: FloatArray = FloatArray(514)
    private var globalMeasCount: Int = 0
    private var recWindowCount: Int = 0
    private var accMeasFlag: Boolean = false
    private var gyroMeasFlag: Boolean = false
    private var magMeasFlag: Boolean = false
    private lateinit var mTextSensorAccX: TextView
    private lateinit var mTextSensorAccY: TextView
    private lateinit var mTextSensorAccZ: TextView
    private lateinit var mTextSensorGyroX: TextView
    private lateinit var mTextSensorGyroY: TextView
    private lateinit var mTextSensorGyroZ: TextView
    private lateinit var mTextSensorMagX: TextView
    private lateinit var mTextSensorMagY: TextView
    private lateinit var mTextSensorMagZ: TextView
    private lateinit var mTextStatusMsg: TextView
    private lateinit var mTextRecWindow: TextView
    private lateinit var logFile: File
    private lateinit var mRecordButton: Button
    private lateinit var selectedMovement: String
    private var recordFlag: Boolean = false
    private var noise: Noise = Noise.real(512)


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recordViewModel =
            ViewModelProvider(this).get(RecordViewModel::class.java)

        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textRecord
        recordViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        mSensorManager = requireActivity().getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensorLinAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mSensorTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        val sensorError = resources.getString(R.string.error_no_sensor)
        if (mSensorAcc == null) {
            mTextSensorAccX.text = sensorError
            mTextSensorAccY.text = sensorError
            mTextSensorAccZ.text = sensorError
        }
        if (mSensorGyro == null) {
            mTextSensorGyroX.text = sensorError
            mTextSensorGyroY.text = sensorError
            mTextSensorGyroZ.text = sensorError
        }
        if (mSensorMag == null) {
            mTextSensorMagX.text = sensorError
            mTextSensorMagY.text = sensorError
            mTextSensorMagZ.text = sensorError
        }

        selectedMovement = "nothing"

        return root
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    private fun stdDev(numArray: FloatArray): Float {
        val sum = numArray.sum()
        val mean = sum / numArray.size
        var variance= 0.0F

        for (num in numArray) {
            variance += (num - mean).pow(2.0F)
        }

        return sqrt(variance / (numArray.size - 1))
    }
    private fun fftAmp(fftArray: FloatArray, ampArray: FloatArray){

    }
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        selectedMovement = parent.getItemAtPosition(pos).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        return
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onResume() {
        super.onResume()

        if (mSensorAcc != null) {
            mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorGyro != null) {
            mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorMag != null) {
            mSensorManager.registerListener(this, mSensorMag, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mTextSensorAccX = requireView().findViewById(R.id.acc_x_raw)
        mTextSensorAccY = requireView().findViewById(R.id.acc_y_raw)
        mTextSensorAccZ = requireView().findViewById(R.id.acc_z_raw)
        mTextSensorGyroX = requireView().findViewById(R.id.gyro_x_raw)
        mTextSensorGyroY = requireView().findViewById(R.id.gyro_y_raw)
        mTextSensorGyroZ = requireView().findViewById(R.id.gyro_z_raw)
        mTextSensorMagX = requireView().findViewById(R.id.mag_x_raw)
        mTextSensorMagY = requireView().findViewById(R.id.mag_y_raw)
        mTextSensorMagZ = requireView().findViewById(R.id.mag_z_raw)
        mRecordButton = requireView().findViewById(R.id.record_button)
        mTextStatusMsg = requireView().findViewById(R.id.status_msg)
        mTextRecWindow = requireView().findViewById(R.id.rec_windows)

        recWindowCount = 0
        mTextRecWindow.text = resources.getString(R.string.rec_windows, recWindowCount)

        if (isExternalStorageWritable()){
            val externalStorageVolumes: Array<out File> =
                ContextCompat.getExternalFilesDirs(requireActivity().applicationContext, null)
            val primaryExternalStorage = externalStorageVolumes[0]
        }

        val spinner: Spinner = requireView().findViewById(R.id.movement_type)
        spinner.onItemSelectedListener = this
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.movement_dropdown,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        mRecordButton.setOnClickListener {
            val inputFilename = "$selectedMovement.csv"
            globalMeasCount = 0
            recWindowCount = 0
            if (!recordFlag){
                mRecordButton.text = "Stop Recording"
                mTextStatusMsg.text = "Recording"
                recordFlag = true

                logFile = File(requireContext().filesDir, inputFilename)
                if(!logFile.exists()){
                    logFile.writeText("Timestamp (HH:mm:ss:SSS),accStdDev,accMean,accFFTPeak,accFFTRatio,magStdDev,gyroStdDev,gyroMean\n\n")
                }
                else{
                    logFile.appendText("\n")
                }
            }
            else{
                mRecordButton.text = "Start Recording"
                recordFlag = false
                mTextRecWindow.text = resources.getString(R.string.rec_windows, recWindowCount)
                val filesDirStr = requireContext().filesDir.toString()
                mTextStatusMsg.text = "Data appended to: $filesDirStr/$inputFilename"
            }
        }
    }


    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(globalMeasCount < accArr.size - 1 && globalMeasCount != (accArr.size/2 - 1)){
            if(accMeasFlag && gyroMeasFlag && magMeasFlag){
                accMeasFlag = false
                gyroMeasFlag = false
                magMeasFlag = false
                globalMeasCount++
            }
            val i = globalMeasCount
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    if(!accMeasFlag){
                        val accX = event.values[0]
                        val accY = event.values[1]
                        val accZ = event.values[2]
                        val absAcc = sqrt(accX.pow(2) + accY.pow(2) + accZ.pow(2))
                        accArr[i] = absAcc

                        mTextSensorAccX.text = resources.getString(R.string.acc_x_raw, accX)
                        mTextSensorAccY.text = resources.getString(R.string.acc_y_raw, accY)
                        mTextSensorAccZ.text = resources.getString(R.string.acc_z_raw, accZ)

                        accMeasFlag = true
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if(!gyroMeasFlag){
                        val gyroX = event.values[0]
                        val gyroY = event.values[1]
                        val gyroZ = event.values[2]
                        val absGyro = sqrt(gyroX.pow(2) + gyroY.pow(2) + gyroZ.pow(2))
                        gyroArr[i] = absGyro

                        mTextSensorGyroX.text = resources.getString(R.string.gyro_x_raw, gyroX)
                        mTextSensorGyroY.text = resources.getString(R.string.gyro_y_raw, gyroY)
                        mTextSensorGyroZ.text = resources.getString(R.string.gyro_z_raw, gyroZ)

                        gyroMeasFlag = true
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    if(!magMeasFlag){
                        val magX = event.values[0]
                        val magY = event.values[1]
                        val magZ = event.values[2]
                        val absMag = sqrt(magX.pow(2) + magY.pow(2) + magZ.pow(2))
                        magArr[i] = absMag

                        mTextSensorMagX.text = resources.getString(R.string.mag_x_raw, magX)
                        mTextSensorMagY.text = resources.getString(R.string.mag_y_raw, magY)
                        mTextSensorMagZ.text = resources.getString(R.string.mag_z_raw, magZ)

                        magMeasFlag = true
                    }
                }
                else -> {}
            }
        }
        else{
            // Window complete -> write to file
            if (recordFlag){
                if(globalMeasCount >= accArr.size - 1){
                    globalMeasCount = 0
                }
                else{
                    globalMeasCount++
                }

                val meanAcc = (accArr.sum()/accArr.size).toBigDecimal().toPlainString()
                val stdDevAcc = stdDev(accArr).toBigDecimal().toPlainString()
                val meanGyro = (gyroArr.sum()/gyroArr.size).toBigDecimal().toPlainString()
                val stdDevGyro = stdDev(gyroArr).toBigDecimal().toPlainString()
                val stdDevMag = stdDev(magArr).toBigDecimal().toPlainString()
                noise.fft(accArr,fftAccArr)
                val fftMaxAcc = fftAccArr.max()
                val fftMaxIndexAcc = fftAccArr.indexOfFirst { fftMaxAcc > 0}
                val fftMaxIndexAccText = fftMaxIndexAcc.toBigDecimal().toPlainString()
                fftAccArr[fftMaxIndexAcc] = 0.0F // To simplify finding second largest elem
                val fftAccSecondPeak = fftAccArr.max()
                val fftPeakRatioAcc = (fftMaxAcc/fftAccSecondPeak).toBigDecimal().toPlainString()

                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
                val current = LocalDateTime.now().format(timeFormatter)
                val dataStr = "$current,$stdDevAcc,$meanAcc,$fftMaxIndexAccText,$fftPeakRatioAcc,$stdDevMag,$stdDevGyro,$meanGyro\n"
                if(recWindowCount > 0){
                    logFile.appendText(dataStr)
                }
                recWindowCount++
                mTextRecWindow.text = resources.getString(R.string.rec_windows, recWindowCount-1)
            }
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
        return
    }
}