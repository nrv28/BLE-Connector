package com.example.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScanActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceListAdapter: DeviceListAdapter
    private lateinit var recyclerView: RecyclerView

    private var scanning = false
    private val scanPeriod = 10000L // Scan for 10 seconds

    private val handler = Handler(Looper.getMainLooper())

    private val PERMISSION_REQUEST_CODE_BLUETOOTH = 1001
    private val PERMISSION_REQUEST_CODE_LOCATION = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Initialize RecyclerView and adapter
        recyclerView = findViewById(R.id.recyclerView)
        deviceListAdapter = DeviceListAdapter { device -> connectToDevice(device) }
        recyclerView.adapter = deviceListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Check and request necessary permissions
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
        } else {
            initializeBluetooth()
            if (!hasLocationPermissions()) {
                requestLocationPermissions()
            }
        }

        // Start and stop scan buttons
        val startScanButton: Button = findViewById(R.id.scan)
        val stopScanButton: Button = findViewById(R.id.stopscan)

        startScanButton.setOnClickListener {
            if (!scanning) {
                startScan()
            }
        }

        stopScanButton.setOnClickListener {
            if (scanning) {
                stopScan()
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED)
    }

    private fun hasLocationPermissions(): Boolean {
        return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ),
            PERMISSION_REQUEST_CODE_BLUETOOTH
        )
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE_LOCATION
        )
    }

    private fun initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All Bluetooth permissions granted, initialize Bluetooth
                    initializeBluetooth()
                    // Check and request location permissions
                    if (!hasLocationPermissions()) {
                        requestLocationPermissions()
                    } else {
                        // Start scanning
                        startScan()
                    }
                } else {
                    // Bluetooth permissions denied, handle it accordingly
                    showAppSettings()
                    Toast.makeText(
                        this,
                        "Bluetooth Permissions required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            PERMISSION_REQUEST_CODE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All location permissions granted, start scanning
                    startScan()
                } else {
                    // Location permissions denied, handle it accordingly
                    Toast.makeText(
                        this,
                        "Location Permissions required.",
                        Toast.LENGTH_SHORT

                    ).show()
                    showAppSettings()
                }
            }
        }
    }


    private fun showAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun startScan() {
        scanning = true
        bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
        handler.postDelayed({
            stopScan()
        }, scanPeriod)
    }

    private fun stopScan() {
        scanning = false
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                val device: BluetoothDevice = scanResult.device
                deviceListAdapter.addDevice(device)
                deviceListAdapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code $errorCode")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        // Stop scanning when connecting to a device
        stopScan()

        // Start DeviceControlActivity and pass device address
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra("device_address", device.address)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "ScanActivity"
    }
}
