# BLE Connector

BLE Connector is an Android application designed to scan, connect, receive, and store data from Bluetooth Low Energy (BLE) devices. This application provides an easy-to-use interface for managing BLE connections and handling data efficiently.

![Screenshot 2024-05-30 012735](https://github.com/lovepathak1224/BLE/assets/155883684/25e68d23-4e75-4fe3-87ca-2d22efd81cb9)


## Features

- **BLE Device Scanning**: Discover nearby BLE devices.
- **Device Connection**: Connect to a selected BLE device.
- **Data Reception**: Receive data from the connected BLE device.
- **Foreground Service**: Maintain connection and data reception even when the app is in the background.
- **Data Storage**: Store received data for later use.

## Installation

To install the BLE Connector app on your Android device, follow these steps:

1. Clone this repository:
    ```sh
    git clone https://github.com/lovepathak1224/BLE.git
    ```
2. Open the project in Android Studio.
3. Build and run the project on your Android device.

## Permissions

The application requires the following permissions:

- `android.permission.BLUETOOTH`
- `android.permission.BLUETOOTH_SCAN`
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.BLUETOOTH_ADMIN`
- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.POST_NOTIFICATIONS`

Ensure these permissions are granted for the app to function correctly.

## Usage

1. **Scan for Devices**: Launch the app and tap on the "Scan" button to start scanning for nearby BLE devices.
2. **Connect to a Device**: Select a device from the list of scanned devices to connect.
3. **Receive Data**: Once connected, the app will start receiving data from the BLE device.
4. **Foreground Service**: The app runs a foreground service to ensure continuous data reception even when the app is in the background. A notification will appear to indicate the service is running.
5. **Store Data**: Received data will be stored locally for future use.

## Code Overview

### Main Components

1. **MainActivity**: Entry point of the application, handles BLE scanning.
2. **DeviceControlActivity**: Manages the connection to the BLE device and displays received data.
3. **BluetoothLeService**: Background service to handle BLE connection and data reception.

### BluetoothLeService

- **initialize()**: Initializes the Bluetooth adapter.
- **connect(address: String)**: Connects to a BLE device with the specified address.
- **enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic)**: Enables notifications for a specific characteristic.
- **createNotification()**: Creates a notification to indicate the service is running.
- **startForegroundService(notification: Notification)**: Starts the service in the foreground.
- **stopForegroundService()**: Stops the foreground service.

## Troubleshooting

- Ensure Bluetooth is enabled on your device.
- Grant all required permissions in the device settings.
- Verify the BLE device is within range and operational.

## Contributing

Contributions are welcome! If you have any suggestions or improvements, feel free to create a pull request or open an issue.
