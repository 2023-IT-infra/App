package com.ItInfraApp.AlertCar.model

import android.Manifest
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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.entity.BluetoothDevice
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class BleService: Service() {
    // Binder given to clients (notice class declaration below)
    private val mBinder: IBinder = LocalBinder()

    private val TAG = "BleService"


    // lazy load bluetoothAdapter and bluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Scanning
    private val bluetoothLeScanner: BluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner!! }

    // Define Scan Settings
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
        .build()

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val hostnameVerifier = HostnameVerifier { hostname, session ->
        if (hostname == "svr.kiwiwip.duckdns.org") true else HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
    }

    private fun fetchAndStartScan() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://svr.kiwiwip.duckdns.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .hostnameVerifier(hostnameVerifier)
                    .build()
            )
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        Log.d(TAG, "Fetching devices from server.")
        apiService.getAllDevices().enqueue(
            object : Callback<List<BluetoothDevice>> {
                override fun onResponse(call: Call<List<BluetoothDevice>>, response: Response<List<BluetoothDevice>>) {
                    Log.d(TAG, "Response: $response")
                    if (response.isSuccessful) {
                        // 서버로부터 가져온 MAC 주소 리스트
                        val devices = response.body() ?: emptyList()
                        Log.d(TAG, "Devices: $devices")
                        // 필터 리스트 생성
                        val scanFilters = devices.mapNotNull { device ->

                            if (device.mac.isNotEmpty()) {
                                ScanFilter.Builder().setDeviceAddress(device.mac).build()
                            } else {
                                null
                            }
                        }

                        // 가져온 필터를 바탕으로 스캔 시작
                        startScanning(scanFilters)
                    } else {
                        // 에러 처리
                        Timber.e("Failed to fetch devices from server. Error code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<BluetoothDevice>>, t: Throwable) {
                    // 에러 처리
                    Timber.e(t, "Failed to fetch devices from server.")
                }
            }
        )
    }


    //private val deviceAddressFilter = listOf("FC:45:C3:A3:09:6A", "FF:FF:70:80:0D:95", "DC:B5:4F:0F:73:AE")


    private val scanResults = mutableListOf<ScanResult>()


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
        fetchAndStartScan()
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
                description = "Alert Channel"
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000)
                setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
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
        scanResults.clear()
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
        intent.putParcelableArrayListExtra("scan_results", scanResults as ArrayList<ScanResult>)
        sendBroadcast(intent)
    }

    private fun startScanning(scanFilters: List<ScanFilter>) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
            Log.d(TAG, "BLE scan started.")
        }
        Log.d(TAG, "BLE scan started.")
    }

    private fun stopScanning() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner.stopScan(scanCallback)
            Log.d(TAG, "BLE scan stopped.")
        }
        Log.d(TAG, "BLE scan stopped.")

    }

    // Device scan Callback
    private val scanCallback: ScanCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Timber.d("onScanResult: $result")
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address

                scanResults[indexQuery] = result
            } else {
                with(result.device) {
                    Timber.d("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
            }

            for (device in scanResults) {
                Log.d(TAG, "Device: ${device.device.name} - ${device.device.address} - ${device.rssi}")
                if(device.rssi> -80) {
                    val notification = NotificationCompat.Builder(this@BleService, "Alert")
                        .setContentTitle("Car Alert!")
                        .setContentText("Car is near you!")
                        .setSmallIcon(R.drawable.mdi__truck_alert_outline)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    with(NotificationManagerCompat.from(this@BleService)) {
                        notify(device.advertisingSid, notification)
                    }
                }
            }
            updateBleScanResult()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan failed with error code: $errorCode")
            updateFailedScanResult( errorCode)
        }


    }


}