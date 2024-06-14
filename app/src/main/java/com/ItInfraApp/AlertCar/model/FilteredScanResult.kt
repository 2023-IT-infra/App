package com.ItInfraApp.AlertCar.model

import android.bluetooth.le.ScanResult
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FilteredScanResult (
    val scanResult: ScanResult,
    val filteredRssi: Int
) : Parcelable