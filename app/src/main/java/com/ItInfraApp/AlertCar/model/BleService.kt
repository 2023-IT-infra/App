package com.ItInfraApp.AlertCar.model

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ItInfraApp.AlertCar.controller.utils.MovingAverageFilter
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.entity.BluetoothDevice
import com.ItInfraApp.AlertCar.controller.utils.KalmanFilter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import kotlin.math.pow
import java.util.concurrent.ConcurrentHashMap

class BleService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    private val binder: IBinder = LocalBinder()
    private val TAG = "BleService"

    private val kalmanFilters = mutableListOf<KalmanFilter>()
    private val movingAvgFilters = mutableListOf<MovingAverageFilter>()

    private var macAddresses = mutableListOf<String>()
    private val txPowers = mutableListOf<Int>()
    private val MAX_RETRY_COUNT = 3
    private var retryCount = 0

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setReportDelay(0)
        .build()

    private val deviceLastSeenMap = ConcurrentHashMap<String, Long>()
    private val SCAN_TIMEOUT_MS = 5000L
    private val filteredScanResults = mutableListOf<FilteredScanResult>()

    private val client = Client.apiService

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action Received: ${intent?.action}")
        if (intent?.action == "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE") {
            Log.d(TAG, "Received Start Foreground Intent")
            startForegroundServiceInternal()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceInternal() {
        initializeScan()
        startForegroundService()
    }

    private fun initializeScan() {
        fetchListMacAddresses()
        startScanning()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "BLE")
            .setContentTitle("차량 감지 활성 중")
            .setContentText("주변의 차량을 감지 중입니다.")
            .setSmallIcon(R.drawable.line_md__bell_alert_loop)
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "BLE", "BLE Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val alertChannel = NotificationChannel(
                "Alert", "Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(ringtone, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        stopScanning()
        kalmanFilters.clear()
        movingAvgFilters.clear()
        filteredScanResults.clear()
        updateBleScanResult()
        super.onDestroy()
    }

    private fun fetchListMacAddresses() {
        Log.d(TAG, "Fetching devices from server.")
        client.getAllDevices().enqueue(object : Callback<List<BluetoothDevice>> {
            override fun onResponse(
                call: Call<List<BluetoothDevice>>,
                response: Response<List<BluetoothDevice>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { devices ->
                        macAddresses.addAll(devices.mapNotNull { it.MAC })
                        txPowers.addAll(devices.map { it.txPower })
                        retryCount = 0
                    } ?: Log.e(TAG, "Response body is null")
                } else {
                    Log.e(TAG, "Response not successful: ${response.errorBody()?.string()}")
                    retryFetchListMacAddresses()
                }
            }

            override fun onFailure(call: Call<List<BluetoothDevice>>, t: Throwable) {
                Timber.e(t, "Failed to fetch devices from server.")
                retryFetchListMacAddresses()
            }
        })
    }

    private fun retryFetchListMacAddresses() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++
            Log.d(TAG, "Retrying to fetch devices from server. Attempt: $retryCount")
            Handler(Looper.getMainLooper()).postDelayed({ fetchListMacAddresses() }, 2000)
        } else {
            Log.e(TAG, "Max retry attempts reached. Failed to fetch devices from server.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        bluetoothLeScanner?.startScan(scanCallback) ?: Log.e(TAG, "BluetoothLeScanner is null")
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback) ?: Log.e(TAG, "BluetoothLeScanner is null")
        Log.d(TAG, "Scanning stopped")
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val address = result.device.address
            val rawRssi = result.rssi.toDouble()
            if (address in macAddresses) {
                val index = macAddresses.indexOf(address)
                val kalman = kalmanFilters.getOrElse(index) {
                    KalmanFilter().also { kalmanFilters.add(it) }
                }
                val avg = movingAvgFilters.getOrElse(index) {
                    MovingAverageFilter().also { movingAvgFilters.add(it) }
                }
                val avgRssi = avg.filter(rawRssi)
                val finalFiltered = kalman.filter(avgRssi)

                val existingResult = filteredScanResults.find { it.address == address }
                if (existingResult != null) {
                    existingResult.filteredRssi = finalFiltered.toInt()
                    existingResult.distance = 10.0.pow((txPowers[index] - finalFiltered) / 20.0)
                } else {
                    filteredScanResults.add(
                        FilteredScanResult(
                            name = result.device.name ?: "Unknown",
                            address = address,
                            txPower = txPowers[index],
                            filteredRssi = finalFiltered.toInt(),
                            distance = 10.0.pow((txPowers[index] - finalFiltered) / 20.0)
                        )
                    )
                }
                deviceLastSeenMap[address] = System.currentTimeMillis()
            }
            checkAndSendAlertNotifications()
            updateBleScanResult()
        }
    }

    private fun removeStaleDevices() {
        val currentTime = System.currentTimeMillis()
        val iterator = deviceLastSeenMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > SCAN_TIMEOUT_MS) {
                val address = entry.key
                val existingResult = filteredScanResults.find { it.address == address }
                if (existingResult != null) {
                    val index = filteredScanResults.indexOf(existingResult)
                    kalmanFilters.removeAt(index)
                    movingAvgFilters.removeAt(index)
                    filteredScanResults.removeAt(index)
                    cancelAlertNotification(index)
                }
                iterator.remove()
            }
        }
    }

    private val staleDeviceHandler = Handler(Looper.getMainLooper())
    private val staleDeviceRunnable = object : Runnable {
        override fun run() {
            removeStaleDevices()
            staleDeviceHandler.postDelayed(this, SCAN_TIMEOUT_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        staleDeviceHandler.post(staleDeviceRunnable)
    }

    private fun checkAndSendAlertNotifications() {
        for (device in filteredScanResults) {
            when {
                device.distance < 1.0 -> alertNotification("Alert", 50, filteredScanResults.indexOf(device), 3)
                device.distance < 5.0 -> alertNotification("Alert", 100, filteredScanResults.indexOf(device), 2)
                device.distance < 10.0 -> alertNotification("Alert", 150, filteredScanResults.indexOf(device), 1)
                else -> {
                    cancelAlertNotification(filteredScanResults.indexOf(device))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun alertNotification(channelId: String, vibratorAmp: Int, index: Int, state: Int) {
        val message = when (state) {
            1 -> "차량이 근처에 있습니다. 주의하세요!"
            2 -> "차량이 정말 가까이 있습니다. 주의하세요!"
            3 -> "차량이 매우 가까이 있습니다. 주의하세요!"
            else -> ""
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("차량 알람!")
            .setContentText(message)
            .setSmallIcon(R.drawable.mdi__truck_alert_outline)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, vibratorAmp.toLong(), 0, vibratorAmp.toLong(), 0, vibratorAmp.toLong()))
            .build()
        with(NotificationManagerCompat.from(this)) {
            notify(index, notification)
        }
    }

    private fun cancelAlertNotification(index: Int) {
        with(NotificationManagerCompat.from(this)) {
            cancel(index)
        }
    }

    private fun updateBleScanResult() {
        val intent = Intent(Actions.ACTION_DEVICE_DATA_CHANGED)
        intent.putParcelableArrayListExtra("scan_results", filteredScanResults as ArrayList<FilteredScanResult>)
        sendBroadcast(intent)
    }

    private fun updateFailedScanResult(errorCode: Int) {
        val intent = Intent(Actions.ACTION_DEVICE_DATA_UPDATED)
        intent.putExtra(Actions.ACTION_DEVICE_DATA_UPDATED, errorCode)
        sendBroadcast(intent)
    }
}
