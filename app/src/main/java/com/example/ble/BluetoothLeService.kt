package com.example.ble

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class BluetoothLeService : Service() {

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.ble.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.ble.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.ble.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.ble.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.ble.EXTRA_DATA"
    }

    private lateinit var bluetoothGatt: BluetoothGatt

    override fun onBind(intent: Intent?): IBinder? {
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService = this@BluetoothLeService
    }

    fun connect(deviceAddress: String) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let { char ->
                val data = char.value
                // Convert the data to string or other appropriate format
                val receivedData = String(data)
                // Send received data to the activity to display in the terminal
                broadcastUpdate(ACTION_DATA_AVAILABLE, receivedData)
            }
        }

        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    broadcastUpdate(ACTION_GATT_CONNECTED, null)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    broadcastUpdate(ACTION_GATT_DISCONNECTED, null)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    val data = characteristic.value
                    val receivedData = String(data)
                    broadcastUpdate(ACTION_DATA_AVAILABLE, receivedData)
                }
                else -> {
                    Log.w(TAG, "onCharacteristicRead failed: $status")
                    broadcastUpdate(ACTION_DATA_AVAILABLE, null) // Notify about failure
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, null)
                }
                else -> {
                    Log.w(TAG, "onServicesDiscovered received: $status")
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, null) // Notify about failure
                }
            }
        }
    }

    private fun broadcastUpdate(action: String, data: String?) {
        val intent = Intent(action)
        data?.let {
            intent.putExtra(EXTRA_DATA, it)
        }
        sendBroadcast(intent)
    }
}
