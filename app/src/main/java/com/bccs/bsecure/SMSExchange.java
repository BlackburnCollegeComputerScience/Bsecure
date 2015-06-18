package com.bccs.bsecure;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
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

    smsBroadcastReceiver onNewMsg = new smsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentlyWorking) {
                if (intent.getStringExtra("number").equals(currentNumber)) {
                    try {
                        dbHelper helper = new dbHelper(context);
                        progressTextView.append("Received g^b%p from " + currentNumber);
                        helper.addKey(intent.getStringExtra("number"),
                                currentSession.packSecret(intent.getStringExtra("body")),
                                currentSession.hashSecret());
                        helper.close();
                        progressTextView.append("Secret key successfully created.");
                        System.out.println("Secret is: ");
                        System.out.println(currentSession.packSecret(intent.getStringExtra("body")));
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
                    handleMessage.send(currentNumber,
                            currentSession.packKey(currentSession.getPublicKey().getEncoded()),
                            getApplicationContext(), true);
                    progressBar.setProgress(50);
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
                        handleMessage.send(currentNumber,
                                currentSession.packKey(currentSession.getPublicKey().getEncoded()),
                                getApplicationContext(), true);
                        progressTextView.append("sent g^a%p to " + currentNumber + "\n");
                        System.out.println("g^a%p = " +
                                currentSession.packKey(currentSession.getPublicKey().getEncoded()));

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
                dbHelper helper = new dbHelper(getApplicationContext());
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
        int id = item.getItemId();

        switch (id) {
            case R.id.action_new:
                openNewMessage();
                return true;
            case R.id.action_contacts:
                openContacts();
                return true;
            case R.id.action_nfc:
                //Checks if the device supports NFC. If not opens the NoNFC activity to communicate
                //through test messaging.
                NfcManager nfcManager = (NfcManager) getApplicationContext().getSystemService(Context.NFC_SERVICE);
                NfcAdapter nfcAdapter = nfcManager.getDefaultAdapter();
                if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                    openNFC();
                } else {
                    openNoNFC();
                }
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_bugReport:
                openBugReport();
                return true;
            case R.id.action_about:
                openAbout();
                return true;
            default:
                openMain();
                return true;
        }

    }

    public void openMain() {
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
    }

    public void openNewMessage() {
        Intent intent = new Intent(this, CreateMessage.class);
        startActivity(intent);
    }

    public void openContacts() {
        Intent intent = new Intent(this, Contacts.class);
        startActivity(intent);
    }

    public void openNFC() {
        Intent intent = new Intent(this, NFC.class);
        startActivity(intent);
    }

    public void openNoNFC() {
        Intent intent = new Intent(this, SMSExchange.class);
        startActivity(intent);
    }

    public void openSettings() {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    public void openBugReport() {
        Intent intent = new Intent(this, BugReport.class);
        startActivity(intent);
    }

    public void openAbout() {
        Intent intent = new Intent(this, About.class);
        startActivity(intent);
    }

}
