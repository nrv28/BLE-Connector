package com.example.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ble.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListAdapter: DeviceListAdapter

    private var scanning = false
    private val scanPeriod = 10000L
    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothEnableRequestCode = 1001
    private val locationPermissionRequestCode = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Initialize RecyclerView and adapter
        deviceListAdapter = DeviceListAdapter { device -> connectToDevice(device) }
        binding.recyclerView.adapter = deviceListAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // Set click listeners for start and stop scan buttons
        binding.btnStartScan.setOnClickListener {
            if (hasLocationPermission()) {
                startScan()
            } else {
                requestLocationPermission()
            }
        }

        binding.btnStopScan.setOnClickListener {
            stopScan()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionRequestCode
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                Toast.makeText(this, "Location permission is required to scan for Bluetooth devices", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScan() {
        if (!scanning) {
            if (!bluetoothAdapter.isEnabled) {
                // Bluetooth is not enabled, prompt the user to turn it on
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, bluetoothEnableRequestCode)
            } else {
                // Bluetooth is enabled, start scanning
                scanning = true
                bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
                handler.postDelayed({
                    stopScan()
                }, scanPeriod)
                Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Scanning is already in progress", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == bluetoothEnableRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth was enabled by the user, start scanning
                startScan()
            } else {
                // Bluetooth was not enabled by the user
                Toast.makeText(this, "Bluetooth must be enabled to scan for devices", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun stopScan() {
        if (scanning) {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No ongoing scanning to stop", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                val device = scanResult.device
                deviceListAdapter.addDevice(device)
                deviceListAdapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code $errorCode")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (scanning) {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra("device_address", device.address)
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
        if (scanning) {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    companion object {
        private const val TAG = "ScanActivity"
    }
}
