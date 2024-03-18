package com.ItInfraApp.AlertCar.model

import android.bluetooth.le.ScanResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _scanResults = MutableLiveData<List<ScanResult>>()
    val scanResults: LiveData<List<ScanResult>> = _scanResults

    fun updateScanResults(results: List<ScanResult>) {
        _scanResults.value = results
    }

    fun clearScanResults() {
        _scanResults.value = emptyList()
    }
}