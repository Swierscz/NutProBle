package com.sierzega.nutproble.ble;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.sierzega.nutproble.R;

import java.util.List;
import java.util.UUID;

public class Connection {
    private final static String TAG = Connection.class.getSimpleName();
    private Activity currentActivity;
    private BleScanner bleScanner;
    private BluetoothLeService bluetoothLeService;
    private String bluetoothAddress;
    private boolean isServiceConnected;

    public Connection(Activity activity) {
        currentActivity = activity;
        initConnection();
    }

    private BluetoothAdapter bluetoothAdapter;

    public BleScanner getBleScanner() {
        return bleScanner;
    }

    //region initialization
    public void initConnection() {
        initializeBluetoothAdapter();
        requestBluetooth();
        bleScanner = new BleScanner(bluetoothAdapter);
        currentActivity.registerReceiver(gattEventsReceiver, makeGattUpdateIntentFilter());
        bindBluetoothLeService();
    }

    private void initializeBluetoothAdapter() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) currentActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void requestBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            currentActivity.startActivityForResult(enableBtIntent, currentActivity.getResources().getInteger(R.integer.request_enable_bluetooth));
        }
    }

    private void bindBluetoothLeService() {
        Intent gattServiceIntent = new Intent(currentActivity, BluetoothLeService.class);
        currentActivity.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                Log.e(TAG, "Bluetooth service cannot be started");
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(bluetoothAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    private final BroadcastReceiver gattEventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                isServiceConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isServiceConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "Services discovered");
                enableNotificationForCharacteristicByUuid("0000ffe1-0000-1000-8000-00805f9b34fb");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //BluetoothLeService.EXTRA_DATA
                String s = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                Log.i(TAG, "Received data is: " + s);
                Log.i(TAG, "Breakpoint");
            }
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void connectToDevice(String aimBtAddress) {
        bluetoothAddress = aimBtAddress;
        if (bluetoothLeService != null) {
            Log.i(TAG, "Connection try started");
            final boolean result = bluetoothLeService.connect(bluetoothAddress);
            Log.i(TAG, "Connect request result=" + result);
        }
    }

    public void disconnectFromCurrentDevice() {
        bluetoothLeService.disconnect();
    }

    public void renewConnection() {
        currentActivity.registerReceiver(gattEventsReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLeService != null) {
            final boolean result = bluetoothLeService.connect(bluetoothAddress);
            Log.i(TAG, "Connect request result=" + result);
        }
    }

    public void pauseConnection() {
        currentActivity.unregisterReceiver(gattEventsReceiver);
    }


    private BluetoothGattCharacteristic currentNotifyCharacteristic = null;


    public void enableNotificationForCharacteristicByUuid(String uuid) {
        enableNotificationForCharacteristic(searchForCharacteristicByUuid(uuid));
    }

    private BluetoothGattCharacteristic searchForCharacteristicByUuid(String uuid) {
        BluetoothGattCharacteristic foundCharacteristic = null;
        List<BluetoothGattService> services = bluetoothLeService.getSupportedGattServices();
        if (services != null && services.size() != 0) {
            Log.i(TAG, "Search for charecteristic started");
            for (BluetoothGattService service : services) {
                BluetoothGattCharacteristic tempChar = service.getCharacteristic(UUID.fromString(uuid));
                if (tempChar != null) {
                    foundCharacteristic = tempChar;
                }

            }
        }
        return foundCharacteristic;
    }

    private void enableNotificationForCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic != null) {
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (currentNotifyCharacteristic != null) {
                    bluetoothLeService.setCharacteristicNotification(
                            currentNotifyCharacteristic, false);
                    currentNotifyCharacteristic = null;
                }
                bluetoothLeService.readCharacteristic(characteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                currentNotifyCharacteristic = characteristic;
                bluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
        }
    }

    public void writeCharacteristicByUuid(String uuid, String data) {
        bluetoothLeService.writeCharacteristic(searchForCharacteristicByUuid(uuid), data);
    }

}








































/*
*
*
    public static RetrofitBuilder getInstance(){
        if(INSTANCE == null)
            synchronized (RetrofitBuilder.class){
                if(INSTANCE == null)
                    INSTANCE = new RetrofitBuilder();
            }
        return INSTANCE;
    }
*
*
*
* */