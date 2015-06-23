package com.bccs.bsecure;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;


/**
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 * <p/>
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 */

public class DeviceListActivity extends Activity {

    //Layout Objects
    private ListView list;
    private DeviceListAdapter adapter;
    private ArrayList<BluetoothDevice> deviceList;
    private TextView statusTv;

    DiffieHellmanKeySession[] session;

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
        statusTv.setMovementMethod(new ScrollingMovementMethod());

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
                    Intent options = new Intent(getApplicationContext(), ExchangeOptions.class);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //onActivityResult is called when a intent finished and comes back to this activity.
        if (requestCode == Constants.REQUEST_KEYS) {
            //If we requested a key exchange
            if (resultCode == RESULT_OK) {
                //Grab the keys from the data packet
                String[] keys = data.getExtras().getStringArray("keys");
                int expireTime = data.getExtras().getInt("expireTime");
                //Pack the keys into a result and send it back to the calling activity
                Intent returnIntent = new Intent();
                returnIntent.putExtra("keys", keys);
                returnIntent.putExtra("expireTime", expireTime);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        }
    }

}
