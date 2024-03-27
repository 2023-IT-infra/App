package com.ItInfraApp.AlertCar.model


import com.ItInfraApp.AlertCar.entity.BluetoothDevice
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("devices")
    fun getAllDevices(): Call<List<BluetoothDevice>>

}