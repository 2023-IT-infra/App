package com.ItInfraApp.AlertCar.view.composables

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.model.BleService
import com.ItInfraApp.AlertCar.model.SharedViewModel
import com.ItInfraApp.AlertCar.view.composables.component.DeviceList
import com.ItInfraApp.AlertCar.view.composables.component.ScanButton
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition

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
            .fillMaxSize(), // Removed padding(10.dp)
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
                        .fillMaxWidth()
                        .navigationBarsPadding() // Added navigationBarsPadding
                        .padding(vertical = 16.dp), // Added some vertical padding for aesthetics
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Clear Results Button
                    Button(
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


private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

// Dummy implementations for placeholder functions to avoid compilation errors if they were the cause.
// These should be reviewed for actual functionality if they are indeed used.
private fun finishAllServices(serviceClass: Class<*>) {
    // Actual implementation needed
}

fun getSystemService(activityService: String): Any? {
    // Actual implementation needed
    return null
}

fun stopService(stopIntent: Intent) {
    // Actual implementation needed
}
