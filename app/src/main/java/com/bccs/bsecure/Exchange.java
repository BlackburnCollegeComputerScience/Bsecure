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
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Exchange handles exchanging of encodings to compute secret keys for each devices in the exchange.
 *
 * @author Shane Nalezyty
 * @version 1.0
 */
public class Exchange extends Activity {

    /**
     * Bluetooth service for controlling connect, accept, and connected threads.
     */
    private BluetoothService bluetoothService;

    /**
     * Session objects for each key.
     */
    private DiffieHellmanKeySession[] session;

    /**
     * Amount of times a key should be used before expiration.
     */
    private int expireCount;

    /**
     * Device we need to connect to.
     */
    private BluetoothDevice device;

    /**
     * Button to start the exchange of data.
     */
    private Button exchangeBtn;

    /**
     * Database to load the user settings from.
     */
    private SCSQLiteHelper database;

    /**
     * Restriction on the SeekBar for security parameters.
     */
    private int minSeek;
    /**
     * Restriction on the SeekBar for security parameters.
     */
    private int maxSeek;
    /**
     * Displays the selected number on the SeekBar
     */
    private TextView amountTv;
    /**
     * SeekBar to display if necessary
     */
    private SeekBar selectionBar;
    /**
     * Multiple uses for displaying information to the user.
     */
    AlertDialog dialog;
    /**
     * Custom dialog to show if security parameters need to be selected.
     */
    Dialog expireDialog;

    private String[] ivs;

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

    /**
     * Makes sure we close the database before we leave the activity.
     */
    @Override
    protected void onDestroy() {
        database.close();
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
     * Checks the protocol code of received packages and continues key exchange.
     */
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

                            ivs = received.getIvs();

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
                            returnIntent.putExtra("ivs", ivs);
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
                            returnIntent.putExtra("ivs", ivs);
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

    /**
     * When no overlap of security parameter range exists this will display a manual selector.
     */
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

    /**
     * Used to send new parameters after manual selection
     */
    private void continueExchange() {
        BluetoothPackage btPack = new BluetoothPackage(expireCount, expireCount, Constants.EXCHANGE_AGREEMENT_FINAL_SELECTION);
        byte[] toSend = getSerializedBytes(btPack);
        bluetoothService.write(toSend);
        startExchange();
    }

    /**
     * Warns the user that the security parameter range of the two devices doesn't overlap.
     * Lets users select new parameters if they are the more secure device.
     */
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

    /**
     * Sends the first packet of actual key exchange after security parameter check.
     */
    private void startExchange() {
        dialog = showDialog("Exchanging " + Constants.KEY_AMOUNT + " keys");
        if (bluetoothService.isServer()) {
            //If this device is the server we will send the first exchange

            //Initialize our session array with our created parameters
            initializeDiffieHellmanSessions(Constants.KEY_AMOUNT);

            //Create a array of strings to hold the public (g^a mod p) key encoding
            String[] publicEncodes = getPublicEncodings(Constants.KEY_AMOUNT);

            ivs = new String[Constants.KEY_AMOUNT];
            for (int i = 0; i < ivs.length; i++) {
                byte[] iv = new byte[16];
                new Random().nextBytes(iv);
                try {
                    ivs[i] = new String(iv, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }


            //Create an object to send over bluetooth containing the public
            //(g^a mod p) encoding and a protocol code to inform the next device
            //of what the stage in our exchange we are at
            BluetoothPackage btPack = new BluetoothPackage(publicEncodes, ivs, Constants.EXCHANGE_FIRST_TRADE);

            //Serialize the bluetooth package to send over the stream
            byte[] toSend = getSerializedBytes(btPack);
            //Write the bytes to the output stream
            bluetoothService.write(toSend);
        }
    }

    /**
     * Checks if a int in the bounds set in parameters.
     * @param toCheck Int to check.
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     * @return Returns true if the int exists between the bounds.
     */
    private boolean inBounds(int toCheck, int lowerBound, int upperBound) {
        return (toCheck >= lowerBound && toCheck <= upperBound);
    }

    /**
     * Constructs session keys from the received public encodings.
     * @param keyAmount Amount of keys.
     * @param receivedKeys Public encodings.
     * @return Returns string array of session keys
     */
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

    /**
     * Constructs the byte array produced from serializing a BluetoothPackage
     * @param bluetoothPackage Package to serialise.
     * @return Byte array representing a Bluetooth Package.
     */
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

    /**
     * Get the public encodings created from our diffie hellman sessions.
     * @param keyAmount Amount of keys
     * @return String array of public encodings
     */
    private String[] getPublicEncodings(int keyAmount) {
        String[] publicEncodes = new String[keyAmount];
        for (int i = 0; i < publicEncodes.length; i++) {
            publicEncodes[i] = session[i].packKey();
        }
        return publicEncodes;
    }

    /**
     * Initializes the array of diffie hellman sessions based on
     * parameters send from the other device.
     * @param keyAmount Amount of keys.
     * @param receivedKeys Received public encodings.
     */
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

    /**
     * Initializes the array of diffie hellman sessions.
     * @param keyAmount amount of keys.
     */
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

    /**
     * Displays a non-cancelable message for the user.
     * @param message Message to display.
     * @return The AlertDialog created.
     */
    private AlertDialog showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Exchange.this);
        builder.setCancelable(false);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    /**
     * Sends this devices security parameters.
     */
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
