package com.example.emtrack

import android.content.Context
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TMDModelHelper(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val MODEL_FILE_NAME = "tmd_model.tflite"

    fun initializeModel() {
        try {
            val modelFile = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            // Handle the initialization error
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val modelFileDescriptor = context.assets.openFd(MODEL_FILE_NAME)
        val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        val startOffset = modelFileDescriptor.startOffset
        val declaredLength = modelFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun runInference(inputData: FloatArray): FloatArray {
        val inputShape = interpreter?.getInputTensor(0)?.shape()
        val outputShape = interpreter?.getOutputTensor(0)?.shape()

        // Ensure input shape matches the model's input requirements
        if (inputShape?.size != 3 || inputShape[1] != 3 || inputShape[2] != 20) {
            throw IllegalArgumentException("Invalid input shape")
        }

        val inputTensor = interpreter?.getInputTensor(0)
        val inputBuffer = ByteBuffer.allocateDirect(inputTensor!!.numBytes())
        inputBuffer.order(ByteOrder.nativeOrder())
        val inputFloatBuffer = inputBuffer.asFloatBuffer()
        inputFloatBuffer.put(inputData)


        val outputTensor = interpreter?.getOutputTensor(0)
        val outputBuffer = ByteBuffer.allocateDirect(outputTensor!!.numBytes())
        outputBuffer.order(ByteOrder.nativeOrder())
        val outputFloatBuffer = outputBuffer.asFloatBuffer()

        // Run inference
        interpreter?.run(inputFloatBuffer, outputBuffer)

        // Retrieve the output data
        val outputData = FloatArray(outputShape!![1])
        outputFloatBuffer.get(outputData)

        return outputData
    }

}