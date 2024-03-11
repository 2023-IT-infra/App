package com.ItInfraApp.AlertCar.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import timber.log.Timber

class BleService: Service() {
    // Binder given to clients (notice class declaration below)
    private val mBinder: IBinder = LocalBinder()

    private val TAG = "BleService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action Received: ${intent?.action}")


        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        startForeground()
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        // Return this instance of BleService so clients can call public methods
        fun getService(): BleService = this@BleService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, "BLE")
            .setContentTitle("BLE Service")
            .setContentText("BLE Service is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(): String {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = "BLE"
        val descriptionText = "BLE Service"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(name, descriptionText, importance)
        channel.importance = NotificationManager.IMPORTANCE_HIGH
        // Register the channel with the system
        val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return name
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
    }

    private fun bleScanUpdate(action: String, scanResults: MutableList<ScanResult>) {
        val intent = Intent(action)
        intent.putExtra("scanResults", ArrayList(scanResults))
        sendBroadcast(intent)
    }

    // Device scan Callback
    private val scanCallback: ScanCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            bleScanUpdate()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan failed with error code: $errorCode")
        }
    }

}