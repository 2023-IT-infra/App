package com.ItInfraApp.AlertCar.view.composables

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.model.BleService
import com.ItInfraApp.AlertCar.model.SharedViewModel
import com.ItInfraApp.AlertCar.view.composables.component.BeaconStatus
import com.ItInfraApp.AlertCar.view.composables.component.MainDropDownMenuButton
import com.ItInfraApp.AlertCar.view.composables.model.MenuOption
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition



@Composable
fun BeaconAlarmMainScreen(viewModel: SharedViewModel, navController: NavController) {

    val scanResults = viewModel.scanResults.observeAsState(initial = emptyList())

    val context = LocalContext.current
    // 폰트 리소스를 FontFamily 객체로 로드

    var currentAnimation by remember { mutableStateOf(R.raw.power_button_on) }

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

    val isScanning: Boolean by remember { mutableStateOf(true) }

    // 메뉴 종류
    val menuOptions = listOf(
        MenuOption("개발자 로그", Icons.Default.Settings) {
            navController.navigate("developerLogScreen")
        }
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars), // Apply system bar padding to the root
        color = MaterialTheme.colorScheme.background
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Spacer(modifier = Modifier.height(22.dp)) // Removed: Replaced by windowInsetsPadding on Surface

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
                        color = Color(0xFF1E293B),
                    )
                )

                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            // 클릭할 때마다:
                            //  a) 이미 실행 중이면 멈추고
                            //  b) 다시 시작
                            val svcIntent = Intent(context, BleService::class.java)
                                .apply { action = "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE" }

                            // 1) 서비스 정지
                            context.stopService(svcIntent)
                            // 2) 서비스 재시작
                            context.startForegroundService(svcIntent)

                            // 애니메이션 리소스 교체
                            currentAnimation =
                                if (isScanning) R.raw.power_button_on
                                else R.raw.power_button
                        }
                        .size(65.dp) //원하는 크기로 조정
                        .fillMaxWidth()
                    ,
                    contentAlignment = Alignment.TopStart
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2) 중간 가변 공간
                Spacer(modifier = Modifier.weight(1f))

                MainDropDownMenuButton(options = menuOptions)

            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(bottom = 24.dp)
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (scanResults.value.isNotEmpty()) {
                        // 비콘이 감지된 경우
                        when (scanResults.value.first().distance) {
                            in 0.0..1.0 -> {
                                // 1m 이내
                                Image(
                                    painter = painterResource(R.drawable.ic__round_warning),
                                    contentDescription = "image description",
                                    contentScale = ContentScale.None,
                                    colorFilter = ColorFilter.tint(Color(0xFFB4070B)),
                                    modifier = Modifier
                                        .padding(0.83333.dp)
                                )

                                Text(
                                    text = "차량이 아주 가까이 있습니다. 조심하십시오",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = FontFamily(Font(R.font.inter)),
                                        fontWeight = FontWeight(600),
                                        color = Color(0xFF1E293B),
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                                )
                            }

                            in 1.0..5.0 -> {
                                // 1m 초과 5m 이내
                                Image(
                                    painter = painterResource(R.drawable.ic__round_warning),
                                    contentDescription = "image description",
                                    contentScale = ContentScale.None,
                                    colorFilter = ColorFilter.tint(Color(0xFFF8730A)),
                                    modifier = Modifier
                                        .padding(0.83333.dp)
                                )

                                Text(
                                    text = "차량이 가까이 있습니다. 조심하십시오",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = FontFamily(Font(R.font.inter)),
                                        fontWeight = FontWeight(600),
                                        color = Color(0xFF1E293B),
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                                )
                            }

                            else -> {
                                // 4m 초과
                                Image(
                                    painter = painterResource(R.drawable.ic__round_warning),
                                    contentDescription = "image description",
                                    contentScale = ContentScale.None,
                                    colorFilter = ColorFilter.tint(Color(0xFF23B14D)),
                                    modifier = Modifier
                                        .padding(0.83333.dp)
                                )

                                Text(
                                    text = "차량이 근처에 있습니다. 조심하십시오",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = FontFamily(Font(R.font.inter)),
                                        fontWeight = FontWeight(600),
                                        color = Color(0xFF1E293B),
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                                )
                            }
                        }
                    } else {
                        // 비콘이 감지되지 않은 경우
                        Text(
                            text = "주변에 차량이 감지된게 없습니다.",
                            style = TextStyle(
                                fontSize = 18.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily(Font(R.font.inter)),
                                fontWeight = FontWeight(600),
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                        )

                    }
                }

//                    LottieAnimation(
//                        composition = composition,
//                        progress = progress,
//                        Modifier
//                            .clickable(
//                                interactionSource = interactionSource,
//                                indication = null, // 클릭 시 리플 효과 제거
//                                onClick = {
//                                    if (isButtonEnabled) {
//                                        isScanning = !isScanning
//
//                                        if (isScanning) {
//                                            if (!isMyServiceRunning(BleService::class.java)) {
//                                                // 서비스 시작
//                                                val startIntent =
//                                                    Intent(context, BleService::class.java)
//                                                startIntent.action =
//                                                    "com.ItInfraApp.AlertCar.ACTION_START_FOREGROUND_SERVICE"
//                                                context.startForegroundService(startIntent)
//                                            }
//                                            currentAnimation = R.raw.power_button_on
//                                            isPlaying = true
//                                        } else {
//                                            // 서비스 정지
//                                            val stopIntent = Intent(context, BleService::class.java)
//                                            context.stopService(stopIntent)
//                                            currentAnimation = R.raw.power_button
//                                            isPlaying = false
//                                        }
//
//                                        // 버튼을 다시 활성화
//                                        isButtonEnabled = true
//                                    }
//                                }
//                            )
//                            .size(400.dp)
//                    )
            }
        }

    }
}