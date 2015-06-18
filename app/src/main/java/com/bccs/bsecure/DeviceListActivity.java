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


public class DeviceListActivity extends Activity {

    //Layout Objects
    private ListView list;
    private DeviceListAdapter adapter;
    private ArrayList<BluetoothDevice> deviceList;
    private TextView statusTv;

    BluetoothService bluetoothService;

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

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            if (bluetoothService.isServer()) {
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
                                //Create an
                                String[] publicEncodes = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < publicEncodes.length; i++) {
                                    publicEncodes[i] = session[i].packKey();
                                }
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_FIRST_TRADE);
                                byte[] toSend = null;
                                try {
                                    toSend = serialize(btPack);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                bluetoothService.write(toSend);
                            }
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
                    BluetoothPackage received = null;
                    try {
                        statusTv.append("Here is the readBuf size: " + msg.arg1 + "\n");
                        received = deserialize(readBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (bluetoothService.isServer()) {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_SECOND_TRADE:
                                String[] keys = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < keys.length; i++) {
                                    try {
                                        keys[i] = session[i].packSecret(received.getKeys()[i]);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            case Constants.EXCHANGE_FINALIZATION_FIRST_ACK:
                                break;
                            case Constants.EXCHANGE_FINALIZATION_SECOND_ACK:
                                break;
                            case Constants.EXCHANGE_ERROR:
                                break;
                        }
                    } else {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_FIRST_TRADE:
                                session = new DiffieHellmanKeySession[Constants.KEY_AMOUNT];
//                                    statusTv.append("Here are the keys we got: \n");
                                for (int i = 0; i < session.length; i++) {
                                    try {
                                        session[i] = new DiffieHellmanKeySession(received.getKeys()[i]);
//                                            statusTv.append(i + ": " + received.getKeys()[i].hashCode() + "\n");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                String[] publicEncodes = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < publicEncodes.length; i++) {
                                    publicEncodes[i] = session[i].packKey();
                                }
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_SECOND_TRADE);
                                byte[] toSend = null;
                                try {
                                    toSend = serialize(btPack);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(toSend.length);
                                bluetoothService.write(toSend);

                                //Compute the session keys

                                String[] keys = new String[Constants.KEY_AMOUNT];
                                for (int i = 0; i < keys.length; i++) {
                                    try {
                                        keys[i] = session[i].packSecret(received.getKeys()[i]);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                statusTv.append("Here are the shared secret keys: \n");
//                                    for (int i = 0; i < keys.length; i++) {
//                                        statusTv.append(i + " " + keys[i].hashCode() + "\n");
//                                    }
                                break;
                            case Constants.EXCHANGE_FINALIZATION_FIRST_ACK:
                                break;
                            case Constants.EXCHANGE_FINALIZATION_SECOND_ACK:
                                break;
                            case Constants.EXCHANGE_ERROR:
                                break;
                        }
                    }
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
