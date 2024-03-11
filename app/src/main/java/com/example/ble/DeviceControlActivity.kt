package com.example.ble

import android.bluetooth.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class DeviceControlActivity : AppCompatActivity() {

    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var deviceAddress: String

    private lateinit var statusTextView: TextView
    private lateinit var terminalTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        deviceAddress = intent.getStringExtra("device_address") ?: ""
        if (deviceAddress.isEmpty()) {
            Log.e(TAG, "Device address not found")
            finish()
            return
        }

        statusTextView = findViewById(R.id.status_text_view)
        terminalTextView = findViewById(R.id.terminal_text_view)

        connectToDevice()
    }

    private fun connectToDevice() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            runOnUiThread {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTING -> updateStatus("Connecting...")
                    BluetoothProfile.STATE_CONNECTED -> {
                        updateStatus("Connected")
                        // Discover services when connected
                        bluetoothGatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> updateStatus("Connection lost")
                    else -> Log.i(TAG, "Connection state changed: $newState")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            runOnUiThread {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        updateStatus("Services discovered")
                        // Enable notifications on the characteristic you want to receive data from
                        enableNotifications()
                    }
                    else -> {
                        updateStatus("Service discovery failed")
                        Log.e(TAG, "Service discovery failed with status: $status")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            runOnUiThread {
                characteristic?.let { char ->
                    val data = char.value.decodeToString()
                    updateTerminal(data)
                }
            }
        }
    }

    private fun updateStatus(status: String) {
        statusTextView.text = status
    }

    private fun updateTerminal(data: String) {
        terminalTextView.append(data + "\n")
    }

    private fun enableNotifications() {
        val service = bluetoothGatt.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        characteristic?.let {
            bluetoothGatt.setCharacteristicNotification(it, true)
            val descriptor = it.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt.writeDescriptor(descriptor)
        }
    }

    companion object {
        private const val TAG = "DeviceControlActivity"
        private val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        private val CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
