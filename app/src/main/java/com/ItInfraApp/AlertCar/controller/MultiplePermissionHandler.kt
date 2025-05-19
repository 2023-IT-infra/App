package com.ItInfraApp.AlertCar.controller

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Handler class for handling multiple permissions related to Bluetooth and Location.
 *
 * @property activity The ComponentActivity instance.
 * @property context The Context instance.
 */
class MultiplePermissionHandler(
    private val activity: ComponentActivity,
    private val context: Context
) {

    private val multiplePermissionResultLauncher by lazy(LazyThreadSafetyMode.NONE) {

        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle result of permission request
            permissions.entries.forEach { entry ->

                val permission = entry.key
                val granted = entry.value
                if (granted) {
                    Toast.makeText(context, "$permission permission granted", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        context,
                        "$permission is required. Please grant this permission.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Request the permission again
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(permission),
                        1
                    )
                }
            }
        }
    }

    /**
     * Checks the BLE-related permissions and requests them if necessary.
     *
     * @param bluetoothAdapter The BluetoothAdapter instance.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun checkBlePermissions(bluetoothAdapter: BluetoothAdapter?) {
        Timber.d("Checking permissions...")

        // Request to enable Bluetooth
        if (!bluetoothAdapter!!.isEnabled) {
            Timber.e("Bluetooth not enabled, terminating...")
            Toast.makeText(context, "Please turn on Bluetooth and try again.", Toast.LENGTH_SHORT)
                .show()
            this.activity.finish()
        }

        val permissionsStateScan = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED


        val permissionsStateConnect = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED

        val permissionsStateLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val permissionsStateBtAdmin = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) != PackageManager.PERMISSION_GRANTED


        val permissions = mapOf(
            Manifest.permission.BLUETOOTH_SCAN to permissionsStateScan,
            Manifest.permission.BLUETOOTH_CONNECT to permissionsStateConnect,
            Manifest.permission.ACCESS_FINE_LOCATION to permissionsStateLocation,
            Manifest.permission.BLUETOOTH_ADMIN to permissionsStateBtAdmin
        )

        val permissionsToRequest = permissions.filterValues { true }.keys.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionResultLauncher.launch(permissionsToRequest)
        } else {
            Timber.d("Permission check passed...")
        }

    }

    fun checkLocationPermissions() {
        val permissionsStateLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val permissionsStateBtAdmin = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) != PackageManager.PERMISSION_GRANTED

        val permissionsStateBt = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) != PackageManager.PERMISSION_GRANTED

        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to permissionsStateLocation,
            Manifest.permission.BLUETOOTH_ADMIN to permissionsStateBtAdmin,
            Manifest.permission.BLUETOOTH to permissionsStateBt
        )

        val permissionsToRequest = permissions.filterValues { true }.keys.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionResultLauncher.launch(permissionsToRequest)
        } else {
            Timber.d("Permission check passed...")
        }

    }


    fun checkInternetPermissions() {
        val permissionsStateInternet = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        ) != PackageManager.PERMISSION_GRANTED

        if (permissionsStateInternet) {
            multiplePermissionResultLauncher.launch(arrayOf(Manifest.permission.INTERNET))
        } else {
            Timber.d("Permission check passed...")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkNotificationPermissions() {
        val permissionsStateNotification = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED

        if (permissionsStateNotification) {
            multiplePermissionResultLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        } else {
            Timber.d("Permission check passed...")
        }
    }
}
