package com.ItInfraApp.AlertCar.model

import android.bluetooth.le.ScanResult
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FilteredScanResult (
    val name: String,
    val address: String,
    val txPower: Int,
    var filteredRssi: Int
) : Parcelable