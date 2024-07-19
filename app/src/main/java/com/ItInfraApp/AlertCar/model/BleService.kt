package com.ItInfraApp.AlertCar.model

import KalmanFilter
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
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
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.entity.BluetoothDevice
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import kotlin.math.pow
import java.util.concurrent.ConcurrentHashMap

class BleService: Service() {
    // Binder given to clients (notice class declaration below)
    private val mBinder: IBinder = LocalBinder()

    private val TAG = "BleService"

    // KalmanFilter 객체 생성
    val kalmanFilters = mutableListOf<KalmanFilter>()

    // Retrofit 객체 생성
    private val client = Client.apiService

    // Mac 주소 리스트
    private var macAddresses = mutableListOf<String>()

    private val txPowers = mutableListOf<Int>()

    private val MAX_RETRY_COUNT = 3
    private var retryCount = 0

    // lazy load bluetoothAdapter and bluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Scanning
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }

    // Define Scan Settings
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // Device last seen map
    private val deviceLastSeenMap = ConcurrentHashMap<String, Long>()
    private val SCAN_TIMEOUT_MS = 5000L




    /**
     * 서버로부터 Mac 주소 리스트를 가져오고, 해당 리스트를 반환한다.
     */
    private fun fetchListMacAddresses() {
        Log.d(TAG, "Fetching devices from server.")
        client.getAllDevices().enqueue(
            object : Callback<List<BluetoothDevice>> {
                override fun onResponse(
                    call: Call<List<BluetoothDevice>>,
                    response: Response<List<BluetoothDevice>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { devices ->
                            macAddresses.addAll(devices.mapNotNull { it.MAC })
                            txPowers.addAll(devices.map { it.txPower })
                            retryCount = 0 // 성공 시 재시도 횟수 초기화
                        } ?: run {
                            Log.e(TAG, "Response body is null")
                        }
                    } else {
                        Log.e(TAG, "Response is not successful: ${response.errorBody()?.string()}")
                        retryFetchListMacAddresses()
                    }
                }

                override fun onFailure(call: Call<List<BluetoothDevice>>, t: Throwable) {
                    Timber.e(t, "Failed to fetch devices from server.")
                    retryFetchListMacAddresses()
                }
            }
        )
    }

    private fun retryFetchListMacAddresses() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++
            Log.d(TAG, "Retrying to fetch devices from server. Attempt: $retryCount")
            // 일정 시간 후 재시도 (예: 2초 후)
            Handler(Looper.getMainLooper()).postDelayed({
                fetchListMacAddresses()
            }, 2000)
        } else {
            Log.e(TAG, "Max retry attempts reached. Failed to fetch devices from server.")
            // 사용자에게 알림 또는 다른 에러 처리 로직 추가 가능
        }
    }

    private val filteredScanResults = mutableListOf<FilteredScanResult>()

    private fun initializeScan() {
        fetchListMacAddresses()
        startScanning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action Received: ${intent?.action}")

        when (intent?.action) {
            "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE" -> {
                Log.d(TAG, "Received Start Foreground Intent")
                startForegroundService()
            }

        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        initializeScan()
        startForeground()
    }

    /**
     * Class used for the client Binder. The Binder object is responsible for returning an instance
     * of "MyService" to the client.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of BleService so clients can call public methods
        fun getService(): BleService = this@BleService
    }

    /**
     * This is how the client gets the IBinder object from the service. It's retrieve by the "ServiceConnection"
     * which you'll see later.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }
    private fun startForeground() {
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
                "BLE",
                "BLE Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val alertChannel = NotificationChannel(
                "Alert",
                "Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                // Ringtone
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
        filteredScanResults.clear()
        updateBleScanResult()
        super.onDestroy()
    }

    private fun updateFailedScanResult(errorCode: Int) {
        val intent = Intent(Actions.ACTION_DEVICE_DATA_UPDATED)
        intent.putExtra(Actions.ACTION_DEVICE_DATA_UPDATED, errorCode)
        sendBroadcast(intent)
    }

    private fun updateBleScanResult() {
        val intent = Intent(Actions.ACTION_DEVICE_DATA_CHANGED)
        intent.putParcelableArrayListExtra("scan_results", filteredScanResults as ArrayList<FilteredScanResult>)
        sendBroadcast(intent)
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

    // Device scan Callback
    private val scanCallback: ScanCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onScanResult: $result")
            handleScanResult(result)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan failed with error code: $errorCode")
            updateFailedScanResult(errorCode)
        }

        @SuppressLint("MissingPermission")
        private fun handleScanResult(result: ScanResult) {
            val address = result.device.address

            if (address in macAddresses) {
                val existingResult = filteredScanResults.find { it.address == address }
                if (existingResult != null) {
                    val index = filteredScanResults.indexOf(existingResult)
                    Log.d(TAG, "Existing result found at index: $filteredScanResults")
                    existingResult.filteredRssi = kalmanFilters[index].filtering(result.rssi.toDouble()).toInt()
                    existingResult.distance = 10.0.pow((txPowers[macAddresses.indexOf(address)] - kalmanFilters[index].filtering(result.rssi.toDouble())) / 20.0).toString().substring(0, 3).toDouble()
                    deviceLastSeenMap[address] = System.currentTimeMillis() // Update last seen time
                } else {
                    val kalmanFilter = KalmanFilter()
                    kalmanFilters.add(kalmanFilter)
                    filteredScanResults.add(
                        FilteredScanResult(
                            name = result.device.name ?: "Unknown",
                            address = address,
                            txPower = txPowers[macAddresses.indexOf(address)],
                            filteredRssi = kalmanFilter.filtering(result.rssi.toDouble()).toInt(),
                            distance = 10.0.pow((txPowers[macAddresses.indexOf(address)] - kalmanFilter.filtering(result.rssi.toDouble())) / 20.0).toString().substring(0, 3).toDouble()
                        )
                    )
                    deviceLastSeenMap[address] = System.currentTimeMillis() // Add to last seen map
                }
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
                    filteredScanResults.removeAt(index)
                    cancelAlertNotification(index)
                }
                iterator.remove()

            }
        }
    }

    // 주기적으로 removeStaleDevices 호출
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
            //소수점 첫째짜리까지만 표시
            val distance = 10.0.pow(((device.txPower - device.filteredRssi) / 20.0)).toString().substring(0, 3)


            when {
                device.filteredRssi > -70 -> alertNotification("Alert", 50, filteredScanResults.indexOf(device), 3)
                device.filteredRssi > -80 -> alertNotification("Alert", 100, filteredScanResults.indexOf(device), 2)
                device.filteredRssi > -90 -> alertNotification("Alert", 150, filteredScanResults.indexOf(device), 1)
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

        // 이전 알림 취소
        with(NotificationManagerCompat.from(this)) {
            // 알림 생성
            notify(index, notification)
        }
    }

    private fun cancelAlertNotification(index: Int) {
        with(NotificationManagerCompat.from(this)) {
            cancel(index)
        }
    }
}
