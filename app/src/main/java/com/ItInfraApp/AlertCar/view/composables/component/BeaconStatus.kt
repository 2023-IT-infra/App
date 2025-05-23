package com.ItInfraApp.AlertCar.view.composables.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ItInfraApp.AlertCar.R
import com.ItInfraApp.AlertCar.model.SharedViewModel

@Composable
fun BeaconStatus(viewModel: SharedViewModel) {

    val scanResults = viewModel.scanResults.observeAsState(initial = emptyList())

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
        // 리스트가 비어 있지 않을 때의 처리 로직
        // 비콘 상태를 표시하는 코드
//            Text("rssi: $rssi dBm",
//                style = TextStyle(
//                fontSize = 14.sp,
//                lineHeight = 20.sp,
//                fontFamily = FontFamily(Font(R.font.inter)),
//                fontWeight = FontWeight(400),
//                color = Color(0xFF334155),
//
//                ),
//                modifier = Modifier
//                    .padding(start = 24.dp, end = 24.dp)
//            )
        // 비콘의 거리를 표시하는 코드
//            Text(
//                text = "거리: ${(resultDistance * 10.0 ).roundToInt() / 10.0 }m",
//                style = TextStyle(
//                    fontSize = 14.sp,
//                    lineHeight = 20.sp,
//                    fontFamily = FontFamily(Font(R.font.inter)),
//                    fontWeight = FontWeight(400),
//                    color = Color(0xFF334155),
//                ),
//                modifier = Modifier
//                    .padding(start = 24.dp, end = 24.dp)
//            )
    }
}