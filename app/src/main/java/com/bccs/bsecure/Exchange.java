package com.bccs.bsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;


public class Exchange extends Activity {

    private BluetoothService bluetoothService;
    private DiffieHellmanKeySession[] session;
    private int expireCount;

    private BluetoothDevice device;

    private ProgressDialog progressDlg;

    private Button exchangeBtn;

    private SCSQLiteHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange);

        //Initialize the progress dialog.
        progressDlg = new ProgressDialog(this);
        progressDlg.setMessage("Exchanging " + Constants.KEY_AMOUNT + " Keys");
        //Don't let it cancel so they don't cancel the key exchange
        progressDlg.setCancelable(false);

        //Get the selected Device
//        device = savedInstanceState.getParcelable("device");
        device = getIntent().getExtras().getParcelable("device");

        bluetoothService = new BluetoothService(handler);
        bluetoothService.start();

        exchangeBtn = (Button) findViewById(R.id.exchangeButton);

        exchangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothService.connect(device);
            }
        });

        database = new SCSQLiteHelper(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_exchange_options, menu);
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
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void showToast(String message) {
        //Shortcut method to display a toast
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if (msg.arg1 == BluetoothService.STATE_CONNECTED) {
                        sendCommunicationParameters();
                    }
                    break;
                case Constants.MESSAGE_READ:
                    BluetoothPackage received = (BluetoothPackage) msg.obj;

                    switch (received.getProtocolCode()) {
                        case Constants.EXCHANGE_AGREEMENT:
                            int[] settings = database.getGeneralSettings();
                            int min = settings[0];
                            int max = settings[1];
                            int receivedMin = received.getMinExpire();
                            int receivedMax = received.getMaxExpire();

                            if (receivedMin > min) {
                                if (inBounds(receivedMin, min, max)) {
                                    expireCount = receivedMin;
                                    startExchange();
                                } else {
                                    expireCount = receivedMin;
                                    AlertDialog.Builder builder = new AlertDialog.Builder(Exchange.this);
                                    builder.setMessage("User has key expiration set outside " +
                                            "your range! Continuing will set key expiration for this " +
                                            "contact at " + expireCount + " text messages. Do you agree to this change?");
                                    builder.setTitle("Warning!");
                                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            BluetoothPackage btPack = new BluetoothPackage(Constants.EXCHANGE_AGREEMENT_ALLOW);
                                            byte[] toSend = getSerializedBytes(btPack);
                                            bluetoothService.write(toSend);
                                            startExchange();
                                        }
                                    });
                                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            BluetoothPackage btPack = new BluetoothPackage(Constants.EXCHANGE_AGREEMENT_DENY);
                                            byte[] toSend = getSerializedBytes(btPack);
                                            bluetoothService.write(toSend);
                                            bluetoothService.stop();
                                            setResult(RESULT_CANCELED);
                                            finish();
                                            onDestroy();
                                            return;
                                        }
                                    });
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    return true;
                                }
                            } else {
                                if (inBounds(min, receivedMin, receivedMax)) {
                                    expireCount = min;
                                    startExchange();
                                } else {
                                    expireCount = min;
                                    //Not in bounds the other device will need to confirm.
                                    return true;
                                }
                            }
                            break;
                        case Constants.EXCHANGE_AGREEMENT_ALLOW:
                            startExchange();
                            break;
                        case Constants.EXCHANGE_AGREEMENT_DENY:
                            bluetoothService.stop();
                            setResult(RESULT_CANCELED);
                            finish();
                            onDestroy();
                            return true;
                    }


                    if (bluetoothService.isServer()) {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_SECOND_TRADE:
                                //If this device is the server and the protocol tells us
                                //that this is the second portion of the key exchange.
                                String[] keys = getSecretKeys(Constants.KEY_AMOUNT, received.getKeys());
                                progressDlg.dismiss();

                                //Pack the keys into a result and send it back to the calling activity
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("keys", keys);
                                returnIntent.putExtra("expireCount", expireCount);
                                setResult(RESULT_OK, returnIntent);
                                bluetoothService.stop();
                                finish();
                                onDestroy();
                                return true;
                        }
                    } else {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_FIRST_TRADE:
                                //If we are the client and the protocol tells us this is the first
                                //half of the exchange

                                //Initialize our array of diffie hellman sessions
                                String[] receivedKeys = received.getKeys();
                                initializeDiffieHellmanSessionsFromKeys(Constants.KEY_AMOUNT, receivedKeys);

                                //Prepair our public (g^b mod p) key encodings to send to the server.
                                String[] publicEncodes = getPublicEncodings(Constants.KEY_AMOUNT);
                                //Create a new bluetooth package with our public keys and
                                //Label the protocol as the second step of the key exchange
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_SECOND_TRADE);

                                //Serialized Bytes
                                byte[] toSend = getSerializedBytes(btPack);
                                //Send the serialized package
                                bluetoothService.write(toSend);

                                //Compute the session keys
                                String[] keys = getSecretKeys(Constants.KEY_AMOUNT, receivedKeys);

                                progressDlg.dismiss();

                                //Pack the keys into a result and send it back to the calling activity
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("keys", keys);
                                returnIntent.putExtra("expireCount", expireCount);
                                setResult(RESULT_OK, returnIntent);
                                bluetoothService.stop();
                                finish();
                                onDestroy();
                                return true;
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

    private void startExchange() {
//        if(!isFinishing()){
        progressDlg.show();
//        }
        if (bluetoothService.isServer()) {
            //If this device is the server we will send the first exchange

            //Initialize our session array with our created parameters
            initializeDiffieHellmanSessions(Constants.KEY_AMOUNT);

            //Create a array of strings to hold the public (g^a mod p) key encoding
            String[] publicEncodes = getPublicEncodings(Constants.KEY_AMOUNT);

            //Create an object to send over bluetooth containing the public
            //(g^a mod p) encoding and a protocol code to inform the next device
            //of what the stage in our exchange we are at
            BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_FIRST_TRADE);

            //Serialize the bluetooth package to send over the stream
            byte[] toSend = getSerializedBytes(btPack);
            //Write the bytes to the output stream
            bluetoothService.write(toSend);
        }
    }

    private boolean inBounds(int toCheck, int lowerBound, int upperBound) {
        return (toCheck >= lowerBound && toCheck <= upperBound);
    }

    private String[] getSecretKeys(int keyAmount, String[] receivedKeys) {
        String[] keys = new String[keyAmount];
        for (int i = 0; i < keys.length; i++) {
            try {
                //Using our private and the public keys we received we compute
                //each key
                keys[i] = session[i].packSecret(receivedKeys[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return keys;
    }

    private byte[] getSerializedBytes(BluetoothPackage bluetoothPackage) {
        //Array of bytes to hold the serialized version of the bluetooth package
        byte[] serialized = null;
        try {
            //Serialize the package into an array of bytes
            serialized = bluetoothPackage.serialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serialized;
    }

    private String[] getPublicEncodings(int keyAmount) {
        String[] publicEncodes = new String[keyAmount];
        for (int i = 0; i < publicEncodes.length; i++) {
            publicEncodes[i] = session[i].packKey();
        }
        return publicEncodes;
    }

    private void initializeDiffieHellmanSessionsFromKeys(int keyAmount, String[] receivedKeys) {
        session = new DiffieHellmanKeySession[keyAmount];
        for (int i = 0; i < session.length; i++) {
            try {
                //Initialize every session against the public key we received
                //from the server.
                session[i] = new DiffieHellmanKeySession(receivedKeys[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeDiffieHellmanSessions(int keyAmount) {
        //Create an array of key sessions for this device
        session = new DiffieHellmanKeySession[keyAmount];
        //Initialize all the key sessions
        for (int i = 0; i < session.length; i++) {
            try {
                session[i] = new DiffieHellmanKeySession();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    private BluetoothPackage getBluetoothPackage(Message msg) {
//        //Grab the array of bytes from the message package
//        byte[] readBuf = (byte[]) msg.obj;
//        //Create a bluetooth package and initialize it by de-serializing the input buffer
//        BluetoothPackage received = null;
//        try {
//            received = deserialize(readBuf);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        return received;
//    }

    private void sendCommunicationParameters() {
        int[] settings = database.getGeneralSettings();
        int min = settings[0];
        int max = settings[1];
        BluetoothPackage bluetoothPackage = new BluetoothPackage(min, max, Constants.EXCHANGE_AGREEMENT);
        byte[] toSend;
        try {
            toSend = bluetoothPackage.serialize();
            bluetoothService.write(toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
