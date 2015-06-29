package com.bccs.bsecure;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

/*
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

/**
 * Bluetooth enabled and disables bluetooth connectivity and manages the discovery of devices.
 *
 * @author Shane Nalezyty
 * @version 1.0
 */
public class Bluetooth extends ActionBarActivity {

    /**
     * Displays if bluetooth is enabled or disabled.
     */
    private TextView btStatus;
    /**
     * Opens a list of devices the device is already paired with
     */
    private Button showBTPairsBtn;
    /**
     * Starts bluetooth discovery
     */
    private Button scanBtn;

    /**
     * Dialog message to inform the user that we are discovering devices, and lets them cancel.
     */
    private ProgressDialog progressDlg;

    /**
     * Array list of devices to send to the DeviceListActivity
     */
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    /**
     * The default bluetooth adapter
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * Boolean to store if bluetooth was already on, or if we turned it on. Used to tell if we
     * need to shut off bluetooth when we are done.
     */
    private boolean bluetoothOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Activity Setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        //Get objects from layout
        btStatus = (TextView) findViewById(R.id.tv_status);
        showBTPairsBtn = (Button) findViewById(R.id.btn_view_paired);
        scanBtn = (Button) findViewById(R.id.btn_scan);

        //Initialize the bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Initialize the progress dialog.
        progressDlg = new ProgressDialog(this);
        progressDlg.setMessage("Scanning...");
        //Remove the default cancel button in favor of our own (Default won't stop discovery)
        progressDlg.setCancelable(false);
        progressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Remove the dialog
                dialog.dismiss();
                //Stop discovery
                bluetoothAdapter.cancelDiscovery();
            }
        });

        showBTPairsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get devices paired to the bluetooth adapter
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                //If no devices are found send a toast displaying such
                if (pairedDevices == null || pairedDevices.size() == 0) {
                    showToast("No Paired Devices Found");
                } else {
                    //Else create a array list of all the devices (You can't pass sets in an Intent)
                    ArrayList<BluetoothDevice> list = new ArrayList<>();
                    list.addAll(pairedDevices);
                    //Initialize and start the Intent to open a list of the device pairs
                    startDeviceList(list);
                }
            }
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //If the user is to fast to click scan right after turing bluetooth on we
                //have to wait for it to finish turing on, or things get messy
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                    return;
                }
                //If the bluetooth adapter is not set to be discoverable by other devices
                if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    //Initialize the popup intent to ask the user to make the device discoverable
                    //for 300 seconds
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    //Start the activity and call onActivityResult() with the request code
                    //REQUEST_ENABLE_BT_DIS when finished
                    startActivityForResult(discoverableIntent, Constants.REQUEST_ENABLE_BT_DIS);
                } else {
                    //If it is already on just start discovery
                    bluetoothAdapter.startDiscovery();
                }
            }
        });

        //If the bluetooth adapter is not enabled
        if (!bluetoothAdapter.isEnabled()) {
            //Initialize the popup intent to ask the user to turn bluetooth on.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //Start the activity and call onActivityResult() with the request code
            //REQUEST_ENABLE_BT when finished
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            //else bluetooth is enabled and we can enabled the buttons and change the status
            showEnabled();
        }

        //Initialize a intent filter for some important actions
        IntentFilter filter = new IntentFilter();
        //The state of the adapter has changed ( On / Off )
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //Discovery has found a device
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //Discovery is started and looking for unpaired devices
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //Discovery has finished
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        //Set our register our receiver to respond on these events.
        registerReceiver(receiver, filter);

    }

    /**
     * Makes sure we cancel discovery when the user leaves the activity.
     */
    @Override
    public void onPause() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isDiscovering()) {
                //Cancel discovery if it is running
                bluetoothAdapter.cancelDiscovery();
            }
        }

        super.onPause();
    }

    /**
     * Un-registers our receiver so the activity won't open out of context
     */
    @Override
    public void onDestroy() {
        //Unregister our receiver before the user leaves the activity
        //Makes sure the activity does not open when a action happens out of context
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    /**
     * Updates display to "Bluetooth is On", and enables the functionality of the buttons
     */
    private void showEnabled() {
        btStatus.setText("Bluetooth is On");
        btStatus.setTextColor(Color.BLUE);
        //Enable buttons
        showBTPairsBtn.setEnabled(true);
        scanBtn.setEnabled(true);
    }

    /**
     * Updates display to "Bluetooth is Off", and disables the functionality of the buttons
     */
    private void showDisabled() {
        btStatus.setText("Bluetooth is Off");
        btStatus.setTextColor(Color.RED);
        //Disable buttons
        showBTPairsBtn.setEnabled(false);
        scanBtn.setEnabled(false);
    }

    /**
     * Short hand method to display a toast.
     * @param message Message that needs to be toasted.
     */
    private void showToast(String message) {
        //Shortcut method to display a toast
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Handles responds to actions we request the user to do.
     * @param requestCode Integer representing what we have requested.
     * @param resultCode Integer representing the result of the request.
     * @param data Intent holding information we may have requested.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //onActivityResult is called when a intent finished and comes back to this activity.
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            //If we requested the user turn bluetooth on
            if (resultCode == RESULT_CANCELED) {
                //If they canceled the request disable the functionality buttons
                showDisabled();
            } else {
                //If they did enable allow them to use the functionality buttons
                showEnabled();
                bluetoothOn = true;
            }
        } else if (requestCode == Constants.REQUEST_ENABLE_BT_DIS) {
            //If we requested the user turn discoverability on
            if (resultCode != RESULT_CANCELED) {
                //If the user turned on discoverability start looking for devices
                bluetoothAdapter.startDiscovery();
            }
        } else if (requestCode == Constants.REQUEST_KEYS) {
            //If we requested a key exchange
            if (resultCode == RESULT_OK) {
                //Grab the keys from the data packet
                String[] keys = data.getExtras().getStringArray("keys");
                int expireCount = data.getExtras().getInt("expireCount");
                //Pack the keys into a result and send it back to the calling activity
                Intent returnIntent = new Intent();
                returnIntent.putExtra("keys", keys);
                returnIntent.putExtra("expireCount", expireCount);
                setResult(RESULT_OK, returnIntent);
                showToast("Exchange complete! 100 keys added to contact.");
                turnOffBluetooth();
                finish();
            } else {
                setResult(RESULT_CANCELED);
                turnOffBluetooth();
                finish();
            }
        }
    }

    /**
     * If bluetooth is on and we turned it on this method will disable it.
     */
    private void turnOffBluetooth() {
        if (bluetoothAdapter.isEnabled() && bluetoothOn) {
            bluetoothAdapter.disable();
        }
    }

    /**
     * Starts the DeviceListActivity.
     * @param list List of devices to pass to the new activity.
     */
    private void startDeviceList(ArrayList<BluetoothDevice> list) {
        //Initialize and start the Intent to open a list of the device pairs
        Intent intent = new Intent(Bluetooth.this, DeviceListActivity.class);
        intent.putParcelableArrayListExtra("device.list", list);
        startActivityForResult(intent, Constants.REQUEST_KEYS);
    }

    /**
     * The receiver looking for state changes in the bluetooth adapter.
     * Responds to state change, discovery start, discover end, and device found.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            //Get the action that resulted in the receiver activation
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //If the Bluetooth adapters state changed
                //Get the state
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    //If turned on make sure functionality buttons are enabled
                    showEnabled();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    //If the user turned bluetooth off display a toast and disable functionality buttons
                    showToast("Bluetooth disabled");
                    showDisabled();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //If we have started to look for devices
                //re-initialize our device list
                deviceList = new ArrayList<>();
                //Show the progress dialog so the user knows we are looking
                progressDlg.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //If discovery has finished looking for devices
                //Remove the progress dialog
                progressDlg.dismiss();
                //Initialise a new intent to list the devices
                startDeviceList(deviceList);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //If we have found a device
                //Get the device from the intent and add it to the list
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(device);
                //Inform the user with a toast
                showToast("Found device " + device.getName());
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so
        // as you specify a parent activity in AndroidManifest.xml.
        onBackPressed();
        return true;
    }
}
