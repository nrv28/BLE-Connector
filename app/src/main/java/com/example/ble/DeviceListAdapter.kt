package com.example.ble

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(private val onItemClick: (BluetoothDevice) -> Unit) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    private val deviceList = mutableListOf<BluetoothDevice>()

    fun addDevice(device: BluetoothDevice) {
        if (!deviceList.contains(device)) {
            deviceList.add(device)
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = deviceList[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = deviceList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(device: BluetoothDevice) {
            deviceName.text = device.name ?: "Unknown device"
            itemView.setOnClickListener {
                onItemClick(device)
            }
        }
    }
}
