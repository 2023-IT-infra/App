package com.ItInfraApp.AlertCar.view.composables.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
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
    val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
    LazyColumn (
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = navBarInsets.calculateBottomPadding()), // Apply bottom padding for nav bar
        clipToPadding = false // Ensure scroll effects are not clipped by padding
    ) {
        items(scanResults) { scanResult ->
            val deviceModel = DeviceModel(
                name = scanResult.name,
                address = scanResult.address,
                rssi = scanResult.filteredRssi
            )
            ExpandableDeviceCard(deviceModel = deviceModel)
        }
    }
}
