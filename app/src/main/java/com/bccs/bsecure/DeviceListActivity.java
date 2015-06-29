package com.bccs.bsecure;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;


/*
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 *
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * DeviceListActivity lists devices send to it in a clean format.
 * Creates pair/un-pair and connect buttons for each device.
 * Manages pairing and un-pairing of devices.
 *
 * @author Shane Nalezyty
 * @version 1.0
 */
public class DeviceListActivity extends Activity {

    /**
     * List to hold devices
     */
    private ListView list;
    /**
     * Adapter to bridge the connection of devices and the list view.
     */
    private DeviceListAdapter adapter;
    /**
     * Array list of devices
     */
    private ArrayList<BluetoothDevice> deviceList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Setup Activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        //Grab the device list from the intent that started the Activity
        deviceList = getIntent().getExtras().getParcelableArrayList("device.list");

        //Initialise list
        list = (ListView) findViewById(R.id.lv_paired);

        //Initialise the adapter
        adapter = new DeviceListAdapter(this);
        adapter.setData(deviceList);

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
                    Intent options = new Intent(DeviceListActivity.this, Exchange.class);
                    options.putExtra("device", device);
                    startActivityForResult(options, Constants.REQUEST_KEYS);
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

    /**
     * Un-registers our receiver so the activity won't launch out of context
     */
    @Override
    public void onDestroy() {
        //Unregister our receiver before the user leaves the activity
        //Makes sure the activity does not open when a action happens out of context
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    /**
     * Short hand method to display a toast.
     *
     * @param message Message that needs to be toasted.
     */
    private void showToast(String message) {
        //Shortcut method to display a toast
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Pairs this device with another device.
     * @param device Device to connect to.
     */
    private void pairDevice(BluetoothDevice device) {
        try {
            //Grab and invoke the method for pairing with this device
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Un-pairs this device with another device.
     * @param device Device to un-pair from.
     */
    private void unpairDevice(BluetoothDevice device) {
        try {
            //Grab and invoke the method for unpairing with this device
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Receiver to respond to the pairing and un-pairing of devices.
     * Sends toasts to inform the user.
     */
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        onBackPressed();
        return true;
    }

    /**
     * If the Exchange activity sends back keys return them to the activity before us.
     * @param requestCode Integer representing what we have requested.
     * @param resultCode Integer representing the result of the request.
     * @param data Intent holding information we may have requested.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //onActivityResult is called when a intent finished and comes back to this activity.
        if (requestCode == Constants.REQUEST_KEYS) {
            //If we requested a key exchange
            if (resultCode == RESULT_OK) {
                //Grab the keys from the data packet
                String[] keys = data.getExtras().getStringArray("keys");
                int expireTime = data.getExtras().getInt("expireCount");
                //Pack the keys into a result and send it back to the calling activity
                Intent returnIntent = new Intent();
                returnIntent.putExtra("keys", keys);
                returnIntent.putExtra("expireCount", expireTime);
                setResult(RESULT_OK, returnIntent);
                finish();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

}
