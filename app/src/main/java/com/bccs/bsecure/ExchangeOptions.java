package com.bccs.bsecure;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;


public class ExchangeOptions extends Activity {

    BluetoothService bluetoothService;
    DiffieHellmanKeySession[] session;

    BluetoothDevice device;

    private ProgressDialog progressDlg;

    EditText keyNumberEt;
    EditText expireTimeEt;

    Button exchangeBtn;

    TextView statusTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_options);

        //Initialize the progress dialog.
        progressDlg = new ProgressDialog(this);
//        progressDlg.setMessage("Exchanging " + Constants.KEY_AMOUNT + " Keys");
        //Don't let it cancel so they don't cancel the key exchange
        progressDlg.setCancelable(false);

        //Get the selected Device
//        device = savedInstanceState.getParcelable("device");
        device = getIntent().getExtras().getParcelable("device");

        bluetoothService = new BluetoothService(handler);
        bluetoothService.start();

        keyNumberEt = (EditText) findViewById(R.id.keyAmountEt);
        expireTimeEt = (EditText) findViewById(R.id.expirationCountEt);

        statusTv = (TextView) findViewById(R.id.statusTv);

        exchangeBtn = (Button) findViewById(R.id.exchangeButton);

        exchangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothService.connect(device);
            }
        });

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

    private void showToast(String message) {
        //Shortcut method to display a toast
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
                            sendCommunicationParameters();
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    BluetoothPackage received = getBluetoothPackage(msg);
                    int keyAmount = Integer.parseInt(keyNumberEt.getText().toString());

                    if (received.getProtocolCode() == Constants.EXCHANGE_AGREEMENT) {
                        //How many text messages should a key last for.
                        int expireCount = Integer.parseInt(expireTimeEt.getText().toString());

                        if (received.getKeyAmount() == keyAmount && received.getExpireCount() == expireCount) {
                            if (bluetoothService.isServer()) {
                                //If this device is the server we will send the first exchange

                                //Initialize our session array with our created parameters
                                initializeDiffieHellmanSessions(keyAmount);

                                //Create a array of strings to hold the public (g^a mod p) key encoding
                                String[] publicEncodes = getPublicEncodings(keyAmount);

                                //Create an object to send over bluetooth containing the public
                                //(g^a mod p) encoding and a protocol code to inform the next device
                                //of what the stage in our exchange we are at
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_FIRST_TRADE);

                                //Serialize the bluetooth package to send over the stream
                                byte[] toSend = getSerializedBytes(btPack);
                                //Write the bytes to the output stream
                                bluetoothService.write(toSend);
                                return true;
                            }
                        } else {
                            //If settings are not the same display an error saying so.
                            statusTv.setText("Error: Settings are not the same!");
                            restartBluetoothService();
                            return false;
                        }

                    }

                    if (bluetoothService.isServer()) {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_SECOND_TRADE:
                                //If this device is the server and the protocol tells us
                                //that this is the second portion of the key exchange.
                                String[] keys = getSecretKeys(keyAmount, received.getKeys());
                                //Pack the keys into a result and send it back to the calling activity
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("keys", keys);
                                setResult(RESULT_OK, returnIntent);

                                statusTv.setText(keys[0]);
                                //Create a bluetooth package to send a confirmation that
                                //we received the packet
                                String[] holder = new String[1];
                                BluetoothPackage firstAck = new BluetoothPackage(holder, Constants.EXCHANGE_FINALIZATION);

                                //serialize the packet
                                byte[] toSend = getSerializedBytes(firstAck);
                                //Stop the service and return the keys
                                bluetoothService.write(toSend);
                                bluetoothService.stop();
                                finish();
                                break;
                        }
                    } else {
                        switch (received.getProtocolCode()) {
                            case Constants.EXCHANGE_FIRST_TRADE:

                                //If we are the client and the protocol tells us this is the first
                                //Packet we have received.

                                //Initialize our array of diffie hellman sessions
                                String[] receivedKeys = received.getKeys();
                                initializeDiffieHellmanSessionsFromKeys(keyAmount, receivedKeys);

                                //Prepair our public (g^b mod p) key encodings to send to the server.
                                String[] publicEncodes = getPublicEncodings(keyAmount);
                                //Create a new bluetooth package with our public keys and
                                //Label the protocol as the second step of the key exchange
                                BluetoothPackage btPack = new BluetoothPackage(publicEncodes, Constants.EXCHANGE_SECOND_TRADE);

                                //Serialized Bytes
                                byte[] toSend = getSerializedBytes(btPack);
                                //Send the serialized package
                                bluetoothService.write(toSend);

                                //Compute the session keys
                                String[] keys = getSecretKeys(keyAmount, receivedKeys);

                                //Pack the keys into a result and send it back to the calling activity
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("keys", keys);
                                setResult(RESULT_OK, returnIntent);
                                break;
                            case Constants.EXCHANGE_FINALIZATION:
                                //stop the service and return the keys
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

    private void restartBluetoothService() {
        bluetoothService.stop();
        bluetoothService = new BluetoothService(handler);
        bluetoothService.start();
    }

    private byte[] getSerializedBytes(BluetoothPackage bluetoothPackage) {
        //Array of bytes to hold the serialized version of the bluetooth package
        byte[] toSend = null;
        try {
            //Serialize the package into an array of bytes
            toSend = bluetoothPackage.serialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toSend;
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

    private BluetoothPackage getBluetoothPackage(Message msg) {
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
        return received;
    }

    private void sendCommunicationParameters() {
        int keyAmount = Integer.parseInt(keyNumberEt.getText().toString());
        int expireCount = Integer.parseInt(expireTimeEt.getText().toString());
        BluetoothPackage bluetoothPackage = new BluetoothPackage(keyAmount, expireCount, Constants.EXCHANGE_AGREEMENT);
        byte[] toSend;
        try {
            toSend = bluetoothPackage.serialize();
            bluetoothService.write(toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
