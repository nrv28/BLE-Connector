package com.example.ble

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ble.adapters.TerminalAdapter
import com.example.ble.databinding.ActivityDeviceControlBinding
import com.example.ble.models.TerminalData
import com.example.ble.services.BluetoothLeService

class DeviceControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceControlBinding
    private lateinit var deviceAddress: String
    private lateinit var terminalAdapter: TerminalAdapter
    private val terminalDataList = mutableListOf<TerminalData>()

    //new code
    private var bluetoothService : BluetoothLeService? = null

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                // call functions on service to check connection and connect to devices
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                // perform device connection
                bluetooth.connect(deviceAddress)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                      updateStatus("Connected")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    updateStatus("Disconnected")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    updateStatus("Services Discovered")
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE ->{
                    val data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                    data?.let { updateTerminal(data) }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothService != null) {
            val result = bluetoothService!!.connect(deviceAddress)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)


        //new code
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Terminal"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_keyboard_backspace_24)
        }

        deviceAddress = intent.getStringExtra("device_address").toString()

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        terminalAdapter = TerminalAdapter(terminalDataList)
        binding.terminalRecyclerView.apply {
            adapter = terminalAdapter
            layoutManager = LinearLayoutManager(this@DeviceControlActivity)
        }
    }


    private fun updateStatus(status: String) {
        binding.statusTextView.text = status
    }

    private fun updateTerminal(data: ByteArray) {
        val receivedString = data.toString(Charsets.UTF_8)
        terminalAdapter.addMessage(receivedString)
        binding.terminalRecyclerView.scrollToPosition(terminalDataList.size - 1)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "DeviceControlActivity"
    }
}
