package com.ItInfraApp.AlertCar.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.ItInfraApp.AlertCar.BuildConfig
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.controller.MultiplePermissionHandler
import com.ItInfraApp.AlertCar.controller.Scanning
import com.ItInfraApp.AlertCar.model.Actions
import com.ItInfraApp.AlertCar.model.BleService
import com.ItInfraApp.AlertCar.model.DeviceModel
import com.ItInfraApp.AlertCar.model.SharedViewModel
import com.ItInfraApp.AlertCar.view.composables.DeviceList
import com.ItInfraApp.AlertCar.view.composables.ScanButton
import com.ItInfraApp.AlertCar.view.theme.AlertCarTheme
import com.ItInfraApp.AlertCar.view.theme.BLEScannerTheme
import kotlinx.coroutines.delay
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SharedViewModel

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var bleIntent: Intent

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
        startBleForegroundService()

        // init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Activity Created...")

        // Set the content to the ble scanner theme starting with the Scanning Screen
        setContent {
            BLEScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScanningScreen(viewModel)
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
            val results = intent.getParcelableArrayListExtra<ScanResult>("scan_results")?.let { ArrayList(it) } ?: arrayListOf()
            viewModel.updateScanResults(results)
        }
    }

    // Entry point for permission checks
    private fun entry() {
        // Check for BLE Permissions
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        multiplePermissionHandler.checkBlePermissions(bluetoothAdapter)
    }

    private fun startBleForegroundService() {
        // Start the BLE Foreground Service
        bleIntent = Intent(this, BleService::class.java)
        ContextCompat.startForegroundService(this, bleIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateReceiver)
    }

    // Scanning Screen Composable
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScanningScreen(viewModel: SharedViewModel) {
        val context = LocalContext.current
        var isScanning: Boolean by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            // Scaffold as outermost on screen
            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = "AlertCar")
                        }
                    )
                }
            ) {
                // Order UI Elements in a column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(paddingValues = it)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,

                        ) {
                        // Box containing a Column with the Devices
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),

                                ) {
                                DeviceList(viewModel = viewModel)
                            }
                        }
                    }
                    // Bottom Row containing two buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Clear Results Button
                        Button(
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 24.dp),
                            onClick = { viewModel.clearScanResults() },
                            content = {
                                Text("Clear Results")
                            }
                        )
                        // Start/Stop Scanning Button
                        ScanButton(
                            scanning = isScanning,
                            onClick = {
                                isScanning = !isScanning
                                if (isScanning) {
                                    // 서비스 시작
                                    val startIntent = Intent(context, BleService::class.java)
                                    startIntent.action = "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE"
                                    context.startForegroundService(startIntent)
                                } else {
                                    // 서비스 정지
                                    val stopIntent = Intent(context, BleService::class.java)
                                    context.stopService(stopIntent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Preview Scanning Screen for Emulator
    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        val dummyScanResults = SharedViewModel()
//        BLEScannerTheme {
//            ScanningScreen(dummyScanResults)
//        }
        AlertCarTheme {
            BeaconAlarmMainScreen(dummyScanResults)
        }
    }

    @Composable
    fun BeaconAlarmMainScreen(viewModel: SharedViewModel) {

        val context = LocalContext.current
        var isScanning: Boolean by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "근처의 비콘 상태",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 비콘 상태 표시
            BeaconStatus(viewModel)

            Divider(Modifier.padding(vertical = 10.dp))

            // 알람 설정
            AlarmSettings()

            Divider(Modifier.padding(vertical = 10.dp))

            // 최근 알람 이력
            RecentAlarms()

            // Bottom Row containing two buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Clear Results Button
                Button(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 24.dp),
                    onClick = { viewModel.clearScanResults() },
                    content = {
                        Text("Clear Results")
                    }
                )
                // Start/Stop Scanning Button
                ScanButton(
                    scanning = isScanning,
                    onClick = {
                        isScanning = !isScanning
                        if (isScanning) {
                            // 서비스 시작
                            val startIntent = Intent(context, BleService::class.java)
                            startIntent.action = "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE"
                            context.startForegroundService(startIntent)
                        } else {
                            // 서비스 정지
                            val stopIntent = Intent(context, BleService::class.java)
                            context.stopService(stopIntent)
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun BeaconStatus(viewModel: SharedViewModel) {
        // 비콘 상태를 표시하는 코드
        Text("rssi:")
        // 추가 비콘 상태 정보...

        DeviceList(viewModel = viewModel)
    }

    @Composable
    fun AlarmSettings() {
        // 알람 설정을 위한 UI
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("알람 활성화")
//        Switch(
//            checked = /* 알람 상태 */,
//            onCheckedChange = { /* 알람 상태 변경 처리 */ }
//        )
        }
    }

    @Composable
    fun RecentAlarms() {
        // 최근 알람 목록을 표시하는 코드
        Column {
            Text("최근 알람", style = MaterialTheme.typography.bodyMedium)
            // 여기에 최근 알람 목록을 동적으로 추가...
        }
    }

    @Composable
    fun SplashScreen(onSplashEnded: () -> Unit) {
        LaunchedEffect(key1 = true) {
            delay(3000) // 3초 대기
            onSplashEnded() // 3초 후 콜백 함수 호출
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // TODO: 로고 이미지 추가 및 로딩 중 표시
                // 로고 이미지 추가
                Image(
                    painter = painterResource(id = R.drawable.beacon),
                    contentDescription = "Beacon Logo",
                    modifier = Modifier.height(100.dp)
                )

                CircularProgressIndicator(
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .height(100.dp)
                        .padding(16.dp)

                ) // 로딩 중 표시
            }
        }
    }
}