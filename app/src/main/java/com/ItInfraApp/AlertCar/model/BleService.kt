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
import android.os.Binder
import android.os.Build
import android.os.IBinder
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


    // lazy load bluetoothAdapter and bluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Scanning
    private val bluetoothLeScanner: BluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner!! }

    // Define Scan Settings
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

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
                        macAddresses.addAll(response.body()?.map { it.mac } ?: emptyList())
                        txPowers.addAll(response.body()?.map { it.txPower } ?: emptyList())
                    }
                }
                override fun onFailure(call: Call<List<BluetoothDevice>>, t: Throwable) {
                    // 에러 처리
                    Timber.e(t, "Failed to fetch devices from server.")
                }
            }
        )

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
            .setContentTitle("ALERT CAR SYSTEM RUNNING")
            .setContentText("Scanning around for Car")
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
                setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
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
            bluetoothLeScanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        bluetoothLeScanner.stopScan(scanCallback)
        Log.d(TAG, "Scanning stopped")
    }


    // Device scan Callback
    private val scanCallback: ScanCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            // results 리스트에 macAddresses 리스트에 포함된 mac 주소가 있는지 확인하고, 있으면 filteredScanResults 리스트에 해당 주소가 있으면 추가하고, 없으면 rssi 값을 업데이트 한다. mac 주소가 없으면 filteredScanResults 리스트에서 삭제한다
            results.forEach { scanResult ->
                if (scanResult.device.address in macAddresses) {
                    filteredScanResults.map { it.address }.contains(scanResult.device.address).let {
                        if (it) {
                            val filteredScanResult = filteredScanResults.find { it.address == scanResult.device.address }
                            filteredScanResult?.filteredRssi =
                                kalmanFilters[filteredScanResults.indexOf(filteredScanResult)].filtering(
                                    scanResult.rssi.toDouble()
                                ).toInt()
                        } else {
                            val kalmanFilter = KalmanFilter()
                            kalmanFilters.add(kalmanFilter)
                            filteredScanResults.add(
                                FilteredScanResult(
                                    name = scanResult.device.name ?: "Unknown",
                                    address = scanResult.device.address,
                                    txPower = txPowers[macAddresses.indexOf(scanResult.device.address)],
                                    filteredRssi = kalmanFilter.filtering(scanResult.rssi.toDouble()).toInt()
                                )
                            )
                        }
                    }
                } else {
                    filteredScanResults.map { it.address }.contains(scanResult.device.address).let {
                        if (it) {
                            val filteredScanResult =
                                filteredScanResults.find { filteredScanResult ->
                                    filteredScanResult.address == scanResult.device.address
                                }
                            filteredScanResults.remove(filteredScanResult)
                            kalmanFilters.removeAt(filteredScanResults.indexOf(filteredScanResult))
                        }
                    }

                }
            }

                fun alertNotification(channelId: String, vibratorAmp: Int, index: Int, state: Int) {
                    val message = when (state) {
                        1 -> "Car is near you!"
                        2 -> "be careful! Car is near you!"
                        3 -> "watch out! Car is very near you!"
                        else -> ""
                    }

                    val notification = NotificationCompat.Builder(this@BleService, channelId)
                        .setContentTitle("Car Alert!")
                        .setContentText(message)
                        .setSmallIcon(R.drawable.mdi__truck_alert_outline)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(
                            longArrayOf(
                                0,
                                vibratorAmp.toLong(),
                                0,
                                vibratorAmp.toLong(),
                                0,
                                vibratorAmp.toLong()
                            )
                        )
                        .build()



                    with(NotificationManagerCompat.from(this@BleService)) {
                        notify(index, notification)
                    }
                }



                for (device in filteredScanResults) {
                    Log.d(
                        TAG,
                        "Device: ${device.name} - ${device.address} - ${device.filteredRssi} - ${device.txPower}"
                    )

                    val distance = 10.0.pow(((device.txPower - device.filteredRssi) / 20.0))
                    Log.d(TAG, "Distance: $distance")

                    if (device.filteredRssi > -70) {
                        alertNotification("Alert", 50, filteredScanResults.indexOf(device), 3)
                    } else if (device.filteredRssi > -80) {
                        alertNotification("Alert", 100, filteredScanResults.indexOf(device), 2)
                    } else if (device.filteredRssi > -90) {
                        alertNotification("Alert", 150, filteredScanResults.indexOf(device), 1)
                    }


                }
                updateBleScanResult()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan failed with error code: $errorCode")
            updateFailedScanResult(errorCode)
        }

    }




}