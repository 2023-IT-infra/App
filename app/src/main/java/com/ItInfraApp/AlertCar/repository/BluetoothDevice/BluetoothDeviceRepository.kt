package com.ItInfraApp.AlertCar.repository.BluetoothDevice

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ItInfraApp.AlertCar.Entity.BluetoothDevice.BluetoothDevice

// interface for the BleMacAddressRepository
@Dao
interface BluetoothDeviceRepository {
    @Query("SELECT * FROM BluetoothDevice")
    fun getAllDevices(): List<BluetoothDevice>

    @Insert
    fun insertDevice(device: BluetoothDevice)
}