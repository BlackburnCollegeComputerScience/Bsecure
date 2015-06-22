package com.bccs.bsecure;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    BluetoothService bluetoothService;
    private ProgressDialog progressDlg;

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

        bluetoothService = new BluetoothService(handler);
        bluetoothService.start();

        //Initialize the progress dialog.
        progressDlg = new ProgressDialog(this);
        progressDlg.setMessage("Exchanging " + Constants.KEY_AMOUNT + " Keys");
        //Don't let it cancel so they don't cancel the key exchange
        progressDlg.setCancelable(false);

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
                    progressDlg.show();
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

    public byte[] serialize(BluetoothPackage bluetoothPackage) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(bluetoothPackage);
        return b.toByteArray();
    }

    public static BluetoothPackage deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return (BluetoothPackage) o.readObject();
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            if (bluetoothService.isServer()) {
                                progressDlg.show();
                                //If this device is the server we will send the first exchange
                                //Create an array of key sessions for this device
                                session = new DiffieHellmanKeySession[Constants.KEY_AMOUNT];
                                //Initialize all the key sessions
                                for (int i = 0; i < session.length; i++) {
                                    try {
                                        session[i] = new DiffieHellmanKeySession();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                //Create a array of strings to hold the public (g^a mod p) key encoding
                                String[] publicEncodes = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < publicEncodes.length; i++) {
                                    publicEncodes[i] = session[i].packKey();
                                }
                                //Create an object to send over bluetooth containing the public
                                //(g^a mod p) encoding and a protocol code to inform the next device
                                //of what the stage in our exchange we are at
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_FIRST_TRADE);
                                //Array of bytes to hold the serialized version of the bluetooth package
                                byte[] toSend = null;
                                try {
                                    //Serialize the package into an array of bytes
                                    toSend = serialize(btPack);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                //Write the bytes to the output stream
                                bluetoothService.write(toSend);
                            }
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    //Grab the array of bytes from the message package
                    byte[] readBuf = (byte[]) msg.obj;
                    //Create a bluetooth package and initialize it by de-serializing the input buffer
                    BluetoothPackage received = null;
                    try {
                        received = deserialize(readBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    if (bluetoothService.isServer()) {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_SECOND_TRADE:
                                //If this device is the server and the protocol tells us
                                //that this is the second portion of the key exchange.
                                String[] keys = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < keys.length; i++) {
                                    try {
                                        //Use our private and the public keys we received
                                        //to create all the session keys.
                                        keys[i] = session[i].packSecret(received.getKeys()[i]);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    //Pack the keys into a result and send it back to the calling activity
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("keys", keys);
                                    setResult(RESULT_OK, returnIntent);

                                    //Create a bluetooth package to send a confirmation that
                                    //we received the packet
                                    String[] holder = new String[1];
                                    BluetoothPackage firstAck = new BluetoothPackage(holder, Constants.EXCHANGE_FINALIZATION);

                                    //serialize the packet
                                    byte[] toSend = null;
                                    try {
                                        toSend = serialize(firstAck);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    //Stop the service and return the keys
                                    bluetoothService.write(toSend);
                                    progressDlg.dismiss();
                                    bluetoothService.stop();
                                    finish();
                                }
                                break;
                        }
                    } else {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_FIRST_TRADE:

                                //If we are the client and the protocol tells us this is the first
                                //Packet we have received.

                                //Initialize our array of diffie hellman sessions
                                session = new DiffieHellmanKeySession[Constants.KEY_AMOUNT];
                                for (int i = 0; i < session.length; i++) {
                                    try {
                                        //Initialize every session against the public key we received
                                        //from the server.
                                        session[i] = new DiffieHellmanKeySession(received.getKeys()[i]);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                //Prepair our public (g^b mod p) key encodings to send to the server.
                                String[] publicEncodes = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < publicEncodes.length; i++) {
                                    publicEncodes[i] = session[i].packKey();
                                }
                                //Create a new bluetooth package with our public keys and
                                //Label the protocol as the second step of the key exchange
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_SECOND_TRADE);
                                //byte array to serialize to
                                byte[] toSend = null;
                                try {
                                    //serialize the package
                                    toSend = serialize(btPack);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                //Send the serialized package
                                bluetoothService.write(toSend);

                                //Compute the session keys
                                String[] keys = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < keys.length; i++) {
                                    try {
                                        //Using our private and the public keys we received we compute
                                        //each key
                                        keys[i] = session[i].packSecret(received.getKeys()[i]);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    //Pack the keys into a result and send it back to the calling activity
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("keys", keys);
                                    setResult(RESULT_OK, returnIntent);
                                }
                                break;
                            case Constants.EXCHANGE_FINALIZATION:
                                //stop the service and return the keys
                                progressDlg.dismiss();
                                bluetoothService.stop();
                                finish();
                                break;
                        }
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (bluetoothService.getState() != BluetoothService.STATE_NONE) {
                        showToast(msg.getData().getString("toast"));
                    }
                    break;
            }
            return true;
        }
    });

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
