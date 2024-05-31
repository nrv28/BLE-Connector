package com.example.ble


import android.annotation.SuppressLint
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
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

class DeviceControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceControlBinding
    private lateinit var deviceAddress: String
    private lateinit var terminalAdapter: TerminalAdapter
    private val terminalDataList = mutableListOf<TerminalData>()
    private var bluetoothService: BluetoothLeService? = null

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                // Call functions on service to check connection and connect to devices
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                // Perform device connection
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
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                    data?.let { updateTerminal(it) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Terminal"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_keyboard_backspace_24)
        }

        deviceAddress = intent.getStringExtra("device_address").toString()

        setupRecyclerView()

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                intent = Intent(this,ScanActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
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
        storeGPSData(receivedString)
        binding.terminalRecyclerView.scrollToPosition(terminalDataList.size - 1)
    }

    @SuppressLint("CheckResult")
    private fun storeGPSData(gpsData: String) {
        val userId = "currentUser"

        Observable.fromCallable {
            val firestore = FirebaseFirestore.getInstance()
            val userDocRef = firestore.collection("users").document(userId)
            val gpsDocRef = firestore.collection("gps").document(userId)

            val latitude = gpsData.substring(2, 11).toDouble()
            val longitude = gpsData.substring(11, 20).toDouble()
            val gpsName = gpsData.substring(0, 2)

            val locationDataMap = hashMapOf(
                "longitude" to longitude,
                "latitude" to latitude,
                "timestamp" to System.currentTimeMillis()
            )

            gpsDocRef.collection("gpsNames").document()
                .set(hashMapOf("name" to gpsName))
                .addOnSuccessListener {
                    Log.d(TAG, "GPS name $gpsName stored successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding GPS name", e)
                }

            userDocRef.collection("gpsData").document(gpsName).collection("locations")
                .add(locationDataMap)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Location data added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding location data", e)
                }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* Handle success */ },
                { error -> Log.e(TAG, "Error storing GPS data", error) }
            )
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }


    companion object {
        private const val TAG = "DeviceControlActivity"
    }
}
