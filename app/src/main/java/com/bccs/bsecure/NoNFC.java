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


public class NoNFC extends ActionBarActivity {

    String currentNumber = "";
    boolean currentlyWorking = false;
    boolean receiverRegistered = false;
    DiffieHellmanKeySession currentSession;
    EditText contactNumText;
    ProgressBar progressBar;
    TextView progressTextView;
    Button startButton;

    smsBroadcastReceiver onNewMsg = new smsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentlyWorking) {
                if (intent.getStringExtra("number").equals(currentNumber)) {
                    try {
                        dbHelper helper = new dbHelper(context);
                        progressTextView.append("Received g^b%p from " + currentNumber);
                        helper.addKey(intent.getStringExtra("number"),
                                currentSession.packSecret(intent.getStringExtra("body")));
                        helper.close();
                        progressTextView.append("Secret key successfully created.");
                        System.out.println("Secret is: ");
                        System.out.println(currentSession.packSecret(intent.getStringExtra("body")));
                        //currentSession.generateSecret(intent.getStringExtra("body"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    currentlyWorking = false;
                }
            } else {
                try {

                    currentNumber = intent.getStringExtra("number");
                    currentSession = new DiffieHellmanKeySession(intent.getStringExtra("body"));
                    currentlyWorking = true;
                    progressTextView.append("Received g^a%p from " + currentNumber);
                    handleMessage.send(currentNumber,
                            currentSession.packKey(currentSession.getPublicKey().getEncoded()),
                            getApplicationContext(), true);
                    System.out.println("Sent g^b%p");
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

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentlyWorking) {

                    try {
                        // Hack to check if proper number
                        //TODO: Replace with contact list dropdown or something else that is pretty

                        Long.parseLong(contactNumText.getText().toString());
                        currentSession = new DiffieHellmanKeySession();
                        currentNumber = contactNumText.getText().toString();
                        currentlyWorking = true;
                        handleMessage.send(currentNumber,
                                currentSession.packKey(currentSession.getPublicKey().getEncoded()),
                                getApplicationContext(), true);
                        progressTextView.append("sent g^a%p to " + currentNumber + "\n");
                        System.out.println("g^a%p = " +
                                currentSession.packKey(currentSession.getPublicKey().getEncoded()));

                        progressTextView.append("Awaiting reply...\n");
                    } catch (Exception e) {e.printStackTrace();}

                }
            }
        });
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
        getMenuInflater().inflate(R.menu.menu_no_nfc, menu);
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
