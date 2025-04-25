package com.ItInfraApp.AlertCar.view.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ItInfraApp.AlertCar.model.DeviceModel
import com.ItInfraApp.AlertCar.model.ScanResultAdapter
import com.ItInfraApp.AlertCar.view.theme.BLEScannerTheme
import com.ItInfraApp.AlertCar.controller.utils.toHex

/**
 * Composable function for rendering a device card.
 *
 * @param deviceModel The device model representing the device.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceCard(deviceModel: DeviceModel) {
    Card(
        modifier = Modifier
            .padding(top = 5.dp, bottom = 5.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp)
        ) {

            Column() {

                Row() {
                    Text(
                        text = deviceModel.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Center Column
                FlowColumn(
                    modifier = Modifier.fillMaxWidth(),
                    ) {
                    Text(
                        text = deviceModel.address,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${deviceModel.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BLEScannerTheme() {
        DeviceCard(
            deviceModel = DeviceModel(
                "Test",
                "Test",
                2
            )
        )
    }
}
