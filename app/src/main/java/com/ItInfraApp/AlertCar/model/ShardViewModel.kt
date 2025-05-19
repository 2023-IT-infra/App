package com.ItInfraApp.AlertCar.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _scanResults = MutableLiveData<List<FilteredScanResult>>(emptyList())
    val scanResults: LiveData<List<FilteredScanResult>> = _scanResults

    fun updateScanResults(results: List<FilteredScanResult>) {
        _scanResults.value = results
    }

    fun clearScanResults() {
        _scanResults.value = emptyList()
    }
}