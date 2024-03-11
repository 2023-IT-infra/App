package com.ItInfraApp.AlertCar.Entity.BluetoothDevice

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BluetoothDevice(
    @PrimaryKey val macAddress: String,
    val deviceName: String?
)