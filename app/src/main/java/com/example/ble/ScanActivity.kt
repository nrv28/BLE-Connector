package com.example.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScanActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListAdapter: DeviceListAdapter
    private lateinit var recyclerView: RecyclerView

    private var scanning = false
    private val scanPeriod = 10000L // Scan for 10 seconds

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Initialize RecyclerView and adapter
        recyclerView = findViewById(R.id.recyclerView)
        deviceListAdapter = DeviceListAdapter { device -> connectToDevice(device) }
        recyclerView.adapter = deviceListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Start scanning for devices
        startScan()
    }

    private fun startScan() {
        scanning = true
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        handler.postDelayed({
            stopScan()
        }, scanPeriod)
    }

    private fun stopScan() {
        scanning = false
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
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

    override fun onStop() {
        super.onStop()
        if (scanning) {
            stopScan()
        }
    }

    companion object {
        private const val TAG = "ScanActivity"
    }
}
