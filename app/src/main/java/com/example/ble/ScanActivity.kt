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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ble.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListAdapter: DeviceListAdapter

    private var scanning = false
    private val scanPeriod = 10000L // Scan for 10 seconds
    private val handler = Handler(Looper.getMainLooper())

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
            startScan()
        }

        binding.btnStopScan.setOnClickListener {
            stopScan()
        }
    }

    private fun startScan() {
        if (!scanning) {
            scanning = true
            bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
            handler.postDelayed({
                stopScan()
            }, scanPeriod)
            Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Scanning is already in progress", Toast.LENGTH_SHORT).show()
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
        stopScan()
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra("device_address", device.address)
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    companion object {
        private const val TAG = "ScanActivity"
    }
}
