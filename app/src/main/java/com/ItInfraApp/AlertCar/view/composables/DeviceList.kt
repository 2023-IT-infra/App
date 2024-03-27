package com.ItInfraApp.AlertCar.view.composables

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ItInfraApp.AlertCar.controller.AdvParser
import com.ItInfraApp.AlertCar.model.DeviceModel
import com.ItInfraApp.AlertCar.model.SharedViewModel

/**
 * Composable function for rendering a list of devices.
 *
 * @param result The list of scan results representing the devices.
 */
@SuppressLint("MissingPermission")
@Composable
fun DeviceList(viewModel: SharedViewModel) {
    val scanResults by viewModel.scanResults.observeAsState(initial = emptyList())
    LazyColumn (
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(scanResults) { scanResult ->
            val deviceModel = DeviceModel(
                name = scanResult.device.name ?: "Unknown",
                address = scanResult.device.address ?: "Unknown",
                rssi = scanResult.rssi,
                bondState = scanResult.device.bondState,
                advertiseFlags = scanResult.scanRecord!!.advertiseFlags,
                rawDataBytes = scanResult.scanRecord!!.bytes,
                parsedBytes = AdvParser().parseBytes(scanResult.scanRecord!!.bytes, scanResult.device.name ?: "Unknown"))
            ExpandableDeviceCard(deviceModel = deviceModel)
        }
    }
}
