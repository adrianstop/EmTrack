package com.example.emtrack

import com.google.gson.Gson

data class SavedData (
    val mode: Int,
    var secPerMode: Long,
    var windowCount: Int,
    var avgConfidence: Float
){
    companion object {
        fun fromJson(json: String): SavedData {
            return Gson().fromJson(json, SavedData::class.java)
        }

        fun toJson(data: SavedData): String {
            return Gson().toJson(data)
        }
    }
}