package com.example.emtrack

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.emtrack.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val INFERENCE_DELAY_MS: Long = 1000
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var tmdModel: TMDModelHelper
    private lateinit var mTextTMState: TextView
    private lateinit var mTextTMAcc: TextView
    private var pendingInferenceFlag = true
    private lateinit var inputData: FloatArray
    private lateinit var dataRecorder : SensorRecorder
    val classes = listOf("Still", "Walking", "Run", "Bike", "Car", "Bus", "Train", "Subway")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_record
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        mTextTMState = findViewById(R.id.text_live_state)
        mTextTMAcc = findViewById(R.id.text_live_accuracy)

        // Initialize the sensor recordings
        dataRecorder = SensorRecorder(this)
        dataRecorder.initSensors()

        // Get constants from sensorRecorder and allocate memory for data array
        inputData = FloatArray(dataRecorder.NUM_SENSORS * dataRecorder.MIN_WINDOWS)

        // Initialize the TMD model
        tmdModel = TMDModelHelper(this)
        tmdModel.initializeModel()

        // Start the inference loop
        startInferenceLoop()

    }
    // Define a Handler and a Runnable
    private val handler = Handler(Looper.getMainLooper())
    private val inferenceRunnable = object : Runnable {
        override fun run() {
            if (!inputData.contentEquals(dataRecorder.data.toFloatArray())){
                pendingInferenceFlag = true
                Log.d(TAG, "New data available")
            }

            if (dataRecorder.dataReadyFlag && pendingInferenceFlag) {
                Log.d(TAG, "Inference started")
                inputData = dataRecorder.data.toFloatArray()
                val outputData = tmdModel.runInference(inputData)
                pendingInferenceFlag = false

                val maxIndex = outputData.indices.maxByOrNull { outputData[it] } ?: -1
                val maxScore = outputData[maxIndex]

                // Update UI elements on the main thread
                runOnUiThread {
                    mTextTMState.text = resources.getString(R.string.live_state, classes[maxIndex])
                    mTextTMAcc.text = resources.getString(R.string.live_state_accuracy, maxScore)
                }
            }

            // Schedule the next inference after a certain delay
            handler.postDelayed(this, INFERENCE_DELAY_MS)
        }
    }

    // Start the inference loop
    private fun startInferenceLoop() {
        handler.postDelayed(inferenceRunnable, 0)
    }

    // Stop the inference loop
    private fun stopInferenceLoop() {
        handler.removeCallbacks(inferenceRunnable)
    }
}