package com.ItInfraApp.AlertCar.view

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.ItInfraApp.AlertCar.BuildConfig
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.controller.MultiplePermissionHandler
import com.ItInfraApp.AlertCar.model.Actions
import com.ItInfraApp.AlertCar.model.BleService
import com.ItInfraApp.AlertCar.model.FilteredScanResult
import com.ItInfraApp.AlertCar.model.SharedViewModel
import com.ItInfraApp.AlertCar.view.composables.DeviceList
import com.ItInfraApp.AlertCar.view.composables.ScanButton
import com.ItInfraApp.AlertCar.view.theme.AlertCarTheme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
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

        // init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Activity Created...")

        // Set the content to the ble scanner theme starting with the Scanning Screen
        setContent {
//            BLEScannerTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    ScanningScreen(viewModel)
//                }
//            }
            AlertCarTheme {
                BeaconAlarmMainScreen(viewModel)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check for BLE Permissions
            multiplePermissionHandler.checkBlePermissions(bluetoothAdapter)
            multiplePermissionHandler.checkInternetPermissions()
        }  else {
            // Check for Internet Permissions
            multiplePermissionHandler.checkLocationPermissions()
            multiplePermissionHandler.checkInternetPermissions()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateReceiver)
    }

    fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun finishAllServices(serviceClass: Class<*>) {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                val stopIntent = Intent(this, serviceClass)
                stopService(stopIntent)
            }
        }
    }

    // Scanning Screen Composable
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScanningScreen(viewModel: SharedViewModel) {
        val context = LocalContext.current
        var isScanning: Boolean by remember { mutableStateOf(false) }
        var isPlaying by remember { mutableStateOf(false) }

        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.power_button)
        )

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
                                    if(!isMyServiceRunning(BleService::class.java)) {
                                        // 서비스 시작
                                        val startIntent = Intent(context, BleService::class.java)
                                        startIntent.action = "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE"
                                        context.startForegroundService(startIntent)
                                    }
                                } else {
                                    finishAllServices(BleService::class.java)
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
        // 폰트 리소스를 FontFamily 객체로 로드
        val MyFontFamily = FontFamily(
            Font(R.font.inter, FontWeight.Normal)
            // 필요에 따라 다른 스타일의 폰트도 추가할 수 있습니다.
        )

        var isButtonEnabled by remember { mutableStateOf(true) }

        var isPlaying by remember { mutableStateOf(false) }

        var currentAnimation by remember { mutableStateOf(R.raw.power_button) }

        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(currentAnimation)
        )

        val interactionSource = remember { MutableInteractionSource() } // 상호작용 상태 추적을 위한 소스

        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = true,
            restartOnPlay = false
        )

        var isScanning: Boolean by remember { mutableStateOf(false) }



        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ){
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, Color(0xFFE0E0E0))
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Alert Car",
                        style = TextStyle(
                            fontSize = 26.sp,
                            lineHeight = 30.sp,
                            //fontFamily = MyFontFamily,
                            //fontWeight = FontWeight.Normal,
                            color = Color(0xFF1E293B),
                        )
                    )

                    Image(
                        painter = painterResource(android.R.drawable.ic_dialog_info),
                        contentDescription = "image description",
                        contentScale = ContentScale.None,
                        colorFilter = ColorFilter.tint(Color(0xFF64748B)),
                        modifier = Modifier
                            .padding(0.83333.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.border(1.dp, Color(0xFFE0E0E0)).padding(bottom = 24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "근처의 비콘 상태",
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily(Font(R.font.inter)),
                            fontWeight = FontWeight(600),
                            color = Color(0xFF1E293B),
                            ),
                        modifier = Modifier
                            .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                    )

                    // 비콘 상태 표시
                    BeaconStatus(viewModel)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = progress,
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null, // 클릭 시 리플 효과 제거
                            onClick = {
                                if (isButtonEnabled) {
                                    isButtonEnabled = false // 버튼 비활성화
                                    isScanning = !isScanning

                                    if (isScanning) {
                                        if (!isMyServiceRunning(BleService::class.java)) {
                                            // 서비스 시작
                                            val startIntent = Intent(context, BleService::class.java)
                                            startIntent.action = "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE"
                                            context.startForegroundService(startIntent)
                                        }
                                        currentAnimation = R.raw.power_button_on
                                        isPlaying = true
                                    } else {
                                        // 서비스 정지
                                        val stopIntent = Intent(context, BleService::class.java)
                                        context.stopService(stopIntent)
                                        currentAnimation = R.raw.power_button
                                        isPlaying = false
                                    }

                                    // 버튼을 다시 활성화
                                    isButtonEnabled = true
                                }
                            }
                        ).size(400.dp)
                    )
                }
            }

        }
    }

    @Composable
    fun BeaconStatus(viewModel: SharedViewModel) {

        val scanResults = viewModel.scanResults.observeAsState(initial = emptyList())
        val resultRssi = viewModel.scanResults.value?.firstOrNull()?.filteredRssi ?: 0



        if (scanResults.value.isEmpty()) {
            // 리스트가 비어 있을 때의 처리 로직
            Text("주변에 비콘이 없습니다.",
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily(Font(R.font.inter)),
                    fontWeight = FontWeight(400),
                    color = Color(0xFF334155),
                    ),
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp)
            )
        } else {
            val rssi = resultRssi
            // 리스트가 비어 있지 않을 때의 처리 로직
            // 비콘 상태를 표시하는 코드
            Text("rssi: $rssi dBm",
                    style = TextStyle(
                    fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(R.font.inter)),
                fontWeight = FontWeight(400),
                color = Color(0xFF334155),

                ),
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp)
            )
        }
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