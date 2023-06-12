package com.example.emtrack.ui.live

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LiveViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Your live status will be shown here"
    }
    val text: LiveData<String> = _text
}