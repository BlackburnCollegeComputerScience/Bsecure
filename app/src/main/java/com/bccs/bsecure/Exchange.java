package com.bccs.bsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class Exchange extends Activity {

    private BluetoothService bluetoothService;
    private DiffieHellmanKeySession[] session;
    private int expireCount;

    private BluetoothDevice device;

    private Button exchangeBtn;

    private SCSQLiteHelper database;

    private int minSeek;
    private int maxSeek;
    private TextView amountTv;
    private SeekBar selectionBar;
    AlertDialog dialog;
    Dialog expireDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange);

        //Get the selected Device
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
                    String keys[];
                    Intent returnIntent;

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
                                    minSeek = max;
                                    maxSeek = receivedMin;
                                    showOutOfRangeWarning();
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
                            dialog = showDialog("User has denied you're less secure key expiration parameters. They are selecting new parameters now.");
                            return true;
                        case Constants.EXCHANGE_AGREEMENT_FINAL_SELECTION:
                            dialog.dismiss();
                            expireCount = received.getMinExpire();
                            startExchange();
                            return true;
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
                            keys = getSecretKeys(Constants.KEY_AMOUNT, receivedKeys);

                            //Pack the keys into a result and send it back to the calling activity
                            returnIntent = new Intent();
                            returnIntent.putExtra("keys", keys);
                            returnIntent.putExtra("expireCount", expireCount);
                            setResult(RESULT_OK, returnIntent);
                            bluetoothService.stop();
                            finish();
                            return true;
                        case Constants.EXCHANGE_SECOND_TRADE:
                            //If this device is the server and the protocol tells us
                            //that this is the second portion of the key exchange.
                            keys = getSecretKeys(Constants.KEY_AMOUNT, received.getKeys());

                            //Pack the keys into a result and send it back to the calling activity
                            returnIntent = new Intent();
                            returnIntent.putExtra("keys", keys);
                            returnIntent.putExtra("expireCount", expireCount);
                            setResult(RESULT_OK, returnIntent);
                            bluetoothService.stop();
                            finish();
                            return true;
                    }
                case Constants.MESSAGE_TOAST:
                    if (bluetoothService.getState() != BluetoothService.STATE_NONE) {
                        showToast(msg.getData().getString("toast"));
                    }
                    return true;
            }
            return true;
        }
    });

    private void pickExpireDialog() {
        expireDialog = new Dialog(Exchange.this);
        expireDialog.setCancelable(false);
        expireDialog.setTitle("Pick an acceptable key expiration:");
        expireDialog.setContentView(R.layout.expire_pick_layout);
        amountTv = (TextView) expireDialog.findViewById(R.id.countTv);
        selectionBar = (SeekBar) expireDialog.findViewById(R.id.selectionBarSkbr);
        selectionBar.setMax(maxSeek);
        selectionBar.setProgress(minSeek);
        amountTv.setText(Integer.toString(minSeek));
        Button startBtn = (Button) expireDialog.findViewById(R.id.okaybtn);

        selectionBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < minSeek) {
                    seekBar.setProgress(minSeek);
                    amountTv.setText(Integer.toString(minSeek));
                } else {
                    amountTv.setText(Integer.toString(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expireCount = selectionBar.getProgress();
                expireDialog.dismiss();
                continueExchange();
            }
        });
        expireDialog.show();
    }

    private void continueExchange() {
        BluetoothPackage btPack = new BluetoothPackage(expireCount, expireCount, Constants.EXCHANGE_AGREEMENT_FINAL_SELECTION);
        byte[] toSend = getSerializedBytes(btPack);
        bluetoothService.write(toSend);
        startExchange();
    }

    private void showOutOfRangeWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Exchange.this);
        builder.setMessage("User has key expiration set outside " +
                "your range! Continuing will set key expiration for this " +
                "contact at " + maxSeek + " text messages. Do you agree to this change?");
        builder.setTitle("Warning!");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothPackage btPack = new BluetoothPackage(Constants.EXCHANGE_AGREEMENT_ALLOW);
                byte[] toSend = getSerializedBytes(btPack);
                bluetoothService.write(toSend);
                expireCount = maxSeek;
                startExchange();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothPackage btPack = new BluetoothPackage(Constants.EXCHANGE_AGREEMENT_DENY);
                byte[] toSend = getSerializedBytes(btPack);
                bluetoothService.write(toSend);
                pickExpireDialog();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startExchange() {
        dialog = showDialog("Exchanging " + Constants.KEY_AMOUNT + " keys");
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

    private AlertDialog showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Exchange.this);
        builder.setCancelable(false);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

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
