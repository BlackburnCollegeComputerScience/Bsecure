package com.bccs.bsecure;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;


public class DeviceListActivity extends Activity {

    //Layout Objects
    private ListView list;
    private DeviceListAdapter adapter;
    private ArrayList<BluetoothDevice> deviceList;
    private TextView statusTv;

    BluetoothService bluetoothService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Setup Activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        //Grab the device list from the intent that started the Activity
        deviceList = getIntent().getExtras().getParcelableArrayList("device.list");

        //Initialise list
        list = (ListView) findViewById(R.id.lv_paired);
        statusTv = (TextView) findViewById(R.id.status);

        //Initialise the adapter
        adapter = new DeviceListAdapter(this);
        adapter.setData(deviceList);

        bluetoothService = new BluetoothService(getApplicationContext(), handler);
        bluetoothService.start();

        adapter.setPairListener(new DeviceListAdapter.OnPairButtonClickListener() {
            @Override
            public void onPairButtonClick(int position) {
                //Grab the device at the position
                BluetoothDevice device = deviceList.get(position);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //If this device is paired then unpair it
                    unpairDevice(device);
                } else {
                    //Else attempt to pair to the device, and display a toast telling the user
                    showToast("Pairing...");
                    pairDevice(device);
                }
            }
        });

        adapter.setConnectListener(new DeviceListAdapter.OnConnectButtonClickListener() {
            @Override
            public void OnConnectButtonClick(int position) {
                //Grab the device at the position
                BluetoothDevice device = deviceList.get(position);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //If this device is paired attempt to connect
                    bluetoothService.connect(device);
                } else {
                    //Else send a toast to inform the user to pair to the device
                    showToast("Please pair to the device first.");
                }
            }
        });

        //Set our list to use the adapter
        list.setAdapter(adapter);

        //Register our receiver to look for changes in bluetooth device pairs
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    @Override
    public void onDestroy() {
        //Unregister our receiver before the user leaves the activity
        //Makes sure the activity does not open when a action happens out of context
        unregisterReceiver(receiver);
        super.onDestroy();
    }


    private void showToast(String message) {
        //Shortcut method to display a toast
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            //Grab and invoke the method for pairing with this device
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            //Grab and invoke the method for unpairing with this device
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            //Get the action that resulted in the receiver activation
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                //If the action is a bond state change
                //Get the current and previous state.
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    //If the state has gone from bonding to bonded display a toast telling
                    //the user that the device has been paired
                    showToast("Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    //If the state has gone from bonded to nothing display a toast telling
                    //the user that the device has been unpaired
                    showToast("Unpaired");
                }
                //Notifies the observers that the underlying data has changed and
                //observers should update themselves
                adapter.notifyDataSetChanged();
            }
        }
    };

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            bluetoothService.write("This is a test".getBytes());
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    statusTv.setText(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    break;
                case Constants.MESSAGE_TOAST:
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
