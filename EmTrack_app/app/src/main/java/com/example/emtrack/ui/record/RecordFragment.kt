package com.example.emtrack.ui.record

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.example.emtrack.RecorderListener
import com.example.emtrack.SensorRecorder
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.paramsen.noise.Noise
import kotlin.math.pow
import kotlin.math.sqrt

class RecordFragment : Fragment(), AdapterView.OnItemSelectedListener, RecorderListener {

    private var _binding: FragmentRecordBinding? = null
    private lateinit var mTextStatusMsg: TextView
    private lateinit var mTextRecWindow: TextView
    private lateinit var dataFile: File
    private lateinit var labelFile: File
    private lateinit var mRecordButton: Button
    private lateinit var selectedMovement: String
    private var recordFlag: Boolean = false
    private lateinit var sensorRecorder: SensorRecorder
    private var localRecordCount: Int = 0

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

        selectedMovement = "nothing"

        return root
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
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
        sensorRecorder.recListener = null
    }
    override fun onResume() {
        super.onResume()

        mRecordButton = requireView().findViewById(R.id.record_button)
        mTextStatusMsg = requireView().findViewById(R.id.status_msg)
        mTextRecWindow = requireView().findViewById(R.id.rec_windows)

        sensorRecorder = SensorRecorder(requireActivity())
        sensorRecorder.initSensors()
        mTextRecWindow.text = resources.getString(R.string.rec_windows, sensorRecorder.recWindowCount)

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
            val recDataFilename = "recorded.csv"
            val recLabelFilename = "labels.csv"

            if (!recordFlag){
                mRecordButton.text = "Stop Recording"
                mTextStatusMsg.text = "Recording"
                recordFlag = true
                Log.d("RecordFragment", "Record flag set to true")

                dataFile = File(requireContext().filesDir, recDataFilename)
                if(!dataFile.exists()){
                    dataFile.writeText("accX, accY, accZ, gyroX, gyroY, gyroZ, magX, magY," +
                            " magZ, orientW, orientX, orientY, orientZ, linAccX, linAccY, linAccZ," +
                            " accMag, gyroMag, magMag, linAccMag")
                }

                labelFile = File(requireContext().filesDir, recLabelFilename)
                if(!labelFile.exists()){
                    labelFile.writeText("label")
                }
            }
            else{
                mRecordButton.text = "Start Recording"
                recordFlag = false
                Log.d("RecordFragment", "Record flag set to false")

                mTextRecWindow.text = resources.getString(R.string.rec_windows, sensorRecorder.recWindowCount)
                val filesDirStr = requireContext().filesDir.toString()
                mTextStatusMsg.text = "Data appended to: $filesDirStr/$recDataFilename"
            }
        }
        sensorRecorder.recListener = this
    }


    override fun onPause() {
        super.onPause()
        sensorRecorder.recListener = null
    }
    override fun onRecWindowsChanged(value: Int) {
        // Perform action in this fragment based on the change of recWindowCount from the sensorRecorder class

        // Write first row of data to dataFile
        if (recordFlag){
            localRecordCount++
            mTextRecWindow.text = resources.getString(R.string.rec_windows, localRecordCount)
            val data = sensorRecorder.data.slice(0 until sensorRecorder.NUM_SENSORS).toMutableList()
            val dataStr = data.joinToString(", ") { "%.10f".format(it) }
            dataFile.appendText("\n$dataStr")

            // Labels are on the form: Null=0, Still=1, Walking=2, Run=3, Bike=4, Car=5, Bus=6, Train=7, Subway=8
            val label = when (selectedMovement) {
                "Still" -> 1
                "Walking" -> 2
                "Run" -> 3
                "Bike" -> 4
                "Car" -> 5
                "Bus" -> 6
                "Train" -> 7
                "Subway" -> 8
                else -> 0
            }
            labelFile.appendText("\n$label")
        }

    }

}