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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.emtrack.databinding.FragmentRecordBinding
import com.example.emtrack.R
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecordFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentRecordBinding? = null
    private lateinit var mSensorManager: SensorManager
    private var mSensorAcc: Sensor? = null
    private lateinit var mTextSensorAccX: TextView
    private lateinit var mTextSensorAccY: TextView
    private lateinit var mTextSensorAccZ: TextView
    private lateinit var mTextStatusMsg: TextView
    private lateinit var logFile: File
    private lateinit var mRecordButton: Button
    private var recordFlag: Boolean = false



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

        val sensorError = resources.getString(R.string.error_no_sensor)
        if (mSensorAcc == null) {
            mTextSensorAccX.text = sensorError
            mTextSensorAccY.text = sensorError
            mTextSensorAccZ.text = sensorError
        }



        return root
    }

    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onResume() {
        super.onResume()

        if (mSensorAcc != null) {
            mSensorManager.registerListener(
                this, mSensorAcc,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        mTextSensorAccX = requireView().findViewById(R.id.acc_x_raw)
        mTextSensorAccY = requireView().findViewById(R.id.acc_y_raw)
        mTextSensorAccZ = requireView().findViewById(R.id.acc_z_raw)
        mRecordButton = requireView().findViewById(R.id.record_button)
        mTextStatusMsg = requireView().findViewById(R.id.status_msg)
        val fileName = requireView().findViewById<EditText>(R.id.filename)

        if (isExternalStorageWritable()){
            val externalStorageVolumes: Array<out File> =
                ContextCompat.getExternalFilesDirs(requireActivity().applicationContext, null)
            val primaryExternalStorage = externalStorageVolumes[0]
        }



        mRecordButton.setOnClickListener {
            val inputFilename:String = fileName.text.toString()

            if (!recordFlag){
                if(inputFilename != getString(R.string.file_name_str)){
                    mRecordButton.text = "Stop Recording"
                    mTextStatusMsg.text = "Recording"
                    recordFlag = true



                    val initFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val current = LocalDateTime.now().format(initFormatter)
                    logFile = File(requireContext().filesDir, inputFilename)
                    logFile.writeText("Accelerometer log data. App start at: $current\n")
                    logFile.appendText("Timestamp, aX, aY, aZ\n")
                }
                else{
                    mTextStatusMsg.text = "C'mon dude..."
                }
            }
            else{
                mRecordButton.text = "Start Recording"
                recordFlag = false
                val filesDirStr = requireContext().filesDir.toString()
                mTextStatusMsg.text = "Data written to: $filesDirStr/$inputFilename"
            }
        }
        mRecordButton.setOnLongClickListener {
            mRecordButton.text = "Ahh, thats better!"
            false
        }
    }


    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorType = event?.sensor?.type
        val accX = event?.values?.get(0)
        val accY = event?.values?.get(1)
        val accZ = event?.values?.get(2)
        val accXtxt = accX?.toBigDecimal()?.toPlainString()
        val accYtxt = accY?.toBigDecimal()?.toPlainString()
        val accZtxt = accZ?.toBigDecimal()?.toPlainString()

        if (recordFlag){
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
            val current = LocalDateTime.now().format(timeFormatter)
            val dataStr = "$current,$accXtxt,$accYtxt,$accZtxt\n"
            logFile.appendText(dataStr)
        }

        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                mTextSensorAccX.text = resources.getString(R.string.acc_x_raw, accX)
                mTextSensorAccY.text = resources.getString(R.string.acc_y_raw, accY)
                mTextSensorAccZ.text = resources.getString(R.string.acc_z_raw, accZ)
            }
            else -> {}
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
        return
    }
}