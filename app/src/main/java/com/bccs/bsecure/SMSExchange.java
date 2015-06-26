package com.bccs.bsecure;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class SMSExchange extends ActionBarActivity {

    String currentNumber = "";
    boolean currentlyWorking = false;
    boolean receiverRegistered = false;
    DiffieHellmanKeySession currentSession;
    EditText contactNumText;
    ProgressBar progressBar;
    TextView progressTextView;
    Button startButton;

    //TEMPORARY THINGS
    TextView manualKey;
    Button manualButton;

    SmsBroadcastReceiver onNewMsg = new SmsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentlyWorking) {
                if (intent.getStringExtra("number").equals(currentNumber)) {
                    try {
                        DbHelper helper = new DbHelper(context);
                        progressTextView.append("Received g^b%p from " + currentNumber);
                        helper.addKey(intent.getStringExtra("number"),
                                currentSession.packSecret(intent.getStringExtra("body")),
                                currentSession.hashSecret());
                        helper.close();
                        progressTextView.append("Secret key successfully created.");
                        //currentSession.generateSecret(intent.getStringExtra("body"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    currentlyWorking = false;
                    progressBar.setProgress(100);
                }
            } else {
                try {

                    currentNumber = intent.getStringExtra("number");
                    currentSession = new DiffieHellmanKeySession(intent.getStringExtra("body"));
                    currentlyWorking = true;
                    contactNumText.setText(currentNumber);
                    progressTextView.append("Received g^a%p from " + currentNumber);
//                    TODO FIX THIS!!!!!!!!!!!
                    HandleMessage.send(0,
                            currentSession.packKey(currentSession.getPublicKey().getEncoded()),
                            getApplicationContext(), true);
                    progressBar.setProgress(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //Filter to catch broadcasts from the main smsBroadcastReceiver when an SMS is received.
    IntentFilter onNewMsgFilter = new IntentFilter("com.bccs.bsecure.msgReceived_DH");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_nfc);
        contactNumText = (EditText) findViewById(R.id.EditText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressTextView = (TextView) findViewById(R.id.progressTextView);
        startButton = (Button) findViewById(R.id.button);

        //TEMPORARY ITEMS
        manualKey = (TextView) findViewById(R.id.manualKey);
        manualButton = (Button) findViewById(R.id.manualButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentlyWorking) {

                    try {
                        // Hack to check if proper number
                        //TODO: Replace text field with contact list dropdown or something else that is pretty

                        Long.parseLong(contactNumText.getText().toString());
                        progressBar.setProgress(0);
                        currentSession = new DiffieHellmanKeySession();
                        currentNumber = contactNumText.getText().toString();
                        currentlyWorking = true;
                        progressTextView.append("g^a%p generated");
                        //TODO FIX THIS
                        HandleMessage.send(0,
                                currentSession.packKey(currentSession.getPublicKey().getEncoded()),
                                getApplicationContext(), true);
                        progressTextView.append("sent g^a%p to " + currentNumber + "\n");

                        progressTextView.append("Awaiting reply...\n");
                        progressBar.setProgress(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        });

        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DbHelper helper = new DbHelper(getApplicationContext());
                String key = manualKey.getText().toString();
                String hash = DiffieHellmanKeySession.toHexString(
                        DiffieHellmanKeySession.getHash(
                                DiffieHellmanKeySession.fromBase64String(key)));
                helper.addKey(contactNumText.getText().toString(), key, hash);
                helper.close();
            }
        });


        currentlyWorking = false;
        currentSession = null;
        registerReceiver();
    }

    @Override
    protected void onResume() {
        registerReceiver();
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver();
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterReceiver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver();
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        registerReceiver();
        super.onRestart();
    }

    @Override
    protected void onStart() {
        registerReceiver();
        super.onStart();
    }

    public void registerReceiver() {
        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiverRegistered = true;
        }
    }

    public void unregisterReceiver() {
        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conservative, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        onBackPressed();
        return true;
    }
}
