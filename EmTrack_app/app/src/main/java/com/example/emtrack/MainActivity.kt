package com.example.emtrack

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.emtrack.databinding.ActivityMainBinding
import com.example.emtrack.ui.live.LiveFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class MainActivity : AppCompatActivity() {


    private val INFERENCE_DELAY_MS: Long = 5000 // Check for new data every 5 seconds
    private val TAG = "MainActivity"
    private val emDataFilename = "data.json"
    private val emptyEmDataFilename = "data_empty.json"

    private lateinit var binding: ActivityMainBinding
    private lateinit var tmdModel: TMDModelHelper
    private lateinit var mTextTMState: TextView
    private lateinit var mTextTMAcc: TextView
    private lateinit var mTextLiveStatus: TextView
    private var pendingInferenceFlag = true
    private lateinit var inputData: FloatArray
    private lateinit var dataRecorder : SensorRecorder
    private lateinit var emDataFile: File
    private var currTimeStamp: Long = System.currentTimeMillis()
    private var lastTimeStamp: Long = System.currentTimeMillis()
    val classes = listOf("Still", "Walking", "Run", "Bike", "Car", "Bus", "Train", "Subway")
    private var maxIndex = 0
    private var maxScore = 0.0f

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
        mTextLiveStatus = findViewById(R.id.text_live_status)

        // Initialize the sensor recordings
        dataRecorder = SensorRecorder(this)
        dataRecorder.initSensors()

        // Get constants from sensorRecorder and allocate memory for data array
        inputData = FloatArray(dataRecorder.NUM_SENSORS * dataRecorder.MIN_WINDOWS)

        // Initialize the TMD model
        tmdModel = TMDModelHelper(this)
        tmdModel.initializeModel()

        emDataFile = File(this.filesDir, emDataFilename)
        if (emDataFile.exists()){
            Log.d(TAG, "Data file exists")
        } else {
            // Load empty data file from assets
            Log.d(TAG, "Data file does not exist. Writing empty skeleton file")
            val gson = Gson()
            val emptyData: String = this.assets.open(emptyEmDataFilename)
                .bufferedReader()
                .use { it.readText() }

            val dataList = gson.fromJson(emptyData, Array<SavedData>::class.java).toList()

            // Write to local files
            val datafile = File(this.filesDir, emDataFilename)
            val writer = FileWriter(datafile)
            val jsonWriteString = gson.toJson(dataList)
            writer.use {
                it.write(jsonWriteString)
            }
        }

        // Start the inference loop
        startInferenceLoop()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopInferenceLoop()

    }
    fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
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

                maxIndex = outputData.indices.maxByOrNull { outputData[it] } ?: -1
                maxScore = outputData[maxIndex]
                currTimeStamp = System.currentTimeMillis()

                val gson = Gson()
                val datafile = File(this@MainActivity.filesDir, emDataFilename)
                val reader = FileReader(datafile)

                val jsonString = reader.readText()
                val dataList = gson.fromJson(jsonString, Array<SavedData>::class.java).toList()

                for (i in dataList.indices) {
                    if (dataList[i].mode == maxIndex) {
                        dataList[i].windowCount += 1
                        dataList[i].secPerMode += ((currTimeStamp - lastTimeStamp) / 1000)
                        dataList[i].avgConfidence = (dataList[i].avgConfidence * (dataList[i].windowCount - 1) + maxScore) / dataList[i].windowCount
                    }
                }
                val writer = FileWriter(datafile)
                val jsonWriteString = gson.toJson(dataList)
                writer.use {
                    it.write(jsonWriteString)
                }

                // Update UI elements on the main thread
                runOnUiThread {
                    Log.d(TAG, "UI thread is ran. Updating UI elements")
                    val currentFragment = getCurrentFragment()
                    Log.d(TAG, "Current fragment is: $currentFragment")
                    if (currentFragment is LiveFragment) {
                        currentFragment.updateUIElements(classes[maxIndex], maxScore, resources.getString(R.string.status_online))
                    }
                }
                lastTimeStamp = currTimeStamp
                Log.d(TAG, "Inference completed, took: ${System.currentTimeMillis() - currTimeStamp} ms")
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