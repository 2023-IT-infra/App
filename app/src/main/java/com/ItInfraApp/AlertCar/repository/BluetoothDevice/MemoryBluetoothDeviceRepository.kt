package com.ItInfraApp.AlertCar.repository.BluetoothDevice

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ItInfraApp.AlertCar.Entity.BluetoothDevice.BluetoothDevice

@Database(entities = [BluetoothDevice::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bluetoothDeviceRepository(): BluetoothDeviceRepository
}