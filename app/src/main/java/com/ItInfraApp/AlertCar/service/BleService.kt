package com.ItInfraApp.AlertCar.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BleService: Service() {
    // Binder given to clients (notice class declaration below)
    private val mBinder: IBinder = LocalBinder()

    private val TAG = "BleService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Log.d(TAG, "Action Received: ${intent?.action}")

        return START_NOT_STICKY
    }

    inner class LocalBinder : Binder() {
        // Return this instance of BleService so clients can call public methods
        fun getService(): BleService = this@BleService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

}