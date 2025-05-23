package com.ItInfraApp.AlertCar.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilteredScanResult (
    val name: String,
    val address: String,
    val txPower: Int,
    var filteredRssi: Int,
    var distance: Double,
    val firstSeen: Long
) : Parcelable