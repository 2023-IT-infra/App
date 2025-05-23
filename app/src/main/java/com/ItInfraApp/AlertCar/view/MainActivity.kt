package com.ItInfraApp.AlertCar.view

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ItInfraApp.AlertCar.BuildConfig
import com.ItInfraApp.AlertCar.controller.MultiplePermissionHandler
import com.ItInfraApp.AlertCar.model.Actions
import com.ItInfraApp.AlertCar.model.BleService
import com.ItInfraApp.AlertCar.model.FilteredScanResult
import com.ItInfraApp.AlertCar.model.SharedViewModel
import com.ItInfraApp.AlertCar.view.composables.BeaconAlarmMainScreen
import com.ItInfraApp.AlertCar.view.composables.DeveloperLogScreen
import com.ItInfraApp.AlertCar.view.composables.SplashScreen
import com.ItInfraApp.AlertCar.view.theme.AlertCarTheme
import timber.log.Timber


class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SharedViewModel

    private lateinit var bluetoothAdapter: BluetoothAdapter

    // Create Multiple Permission Handler to handle all the required permissions
    private val multiplePermissionHandler: MultiplePermissionHandler by lazy {
        MultiplePermissionHandler(this, this)
    }

    // On create function
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        // init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Activity Created...")


        // Set the content to the ble scanner theme starting with the Scanning Screen
        setContent {

            AlertCarTheme {
                // 앱 시작 시 SplashScreen을 보여주고, 3초 후 메인 화면으로 전환
                var showSplashScreen by remember { mutableStateOf(true) }

                SplashScreen(onSplashEnded = { showSplashScreen = false })

                if (!showSplashScreen) {
                    // 서비스 시작
                    val startIntent =
                        Intent(LocalContext.current, BleService::class.java)
                    startIntent.action =
                        "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE"
                    LocalContext.current.startForegroundService(startIntent)

                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "BeaconAlarmMainScreen") {
                        composable("BeaconAlarmMainScreen"){
                            BeaconAlarmMainScreen(viewModel, navController)
                        }
                        composable("developerLogScreen"){
                            DeveloperLogScreen(viewModel, navController)
                        }

                    }
                }
            }
        }

        Timber.d("Content Set...")

        registerReceiver(updateReceiver, IntentFilter(Actions.ACTION_DEVICE_DATA_CHANGED),
            RECEIVER_NOT_EXPORTED
        )

        try {
            entry()
        } catch (e: Exception) {
            Timber.tag(e.toString())
        }


    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val results = intent.getParcelableArrayListExtra<FilteredScanResult>("scan_results")?.let { ArrayList(it) } ?: arrayListOf()
            viewModel.updateScanResults(results)
        }
    }

    // Entry point for permission checks
    private fun entry() {
        // Check for BLE Permissions
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // 빌드 번호 확인
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            multiplePermissionHandler.checkNotificationPermissions()
            // Check for BLE Permissions
            multiplePermissionHandler.checkBlePermissions(bluetoothAdapter)
            multiplePermissionHandler.checkInternetPermissions()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check for BLE Permissions
            multiplePermissionHandler.checkBlePermissions(bluetoothAdapter)
            multiplePermissionHandler.checkInternetPermissions()
        } else {
            // Check for Internet Permissions
            multiplePermissionHandler.checkLocationPermissions()
            multiplePermissionHandler.checkInternetPermissions()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateReceiver)
    }

    // Preview Scanning Screen for Emulator
    @SuppressLint("ViewModelConstructorInComposable")
    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        val dummyScanResults = SharedViewModel()
//        BLEScannerTheme {
//            ScanningScreen(dummyScanResults)
//        }
        AlertCarTheme {
            BeaconAlarmMainScreen(navController = rememberNavController(), viewModel = dummyScanResults)
        }
    }
}