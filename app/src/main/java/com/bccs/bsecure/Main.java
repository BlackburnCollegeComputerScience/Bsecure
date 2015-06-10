package com.bccs.bsecure;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/* Note -


 */
public class Main extends AppCompatActivity {

    //Global class variables

    //The SQLite DB helper - traverses outbox
    //The adapter to use when displaying to the screen.
    private ArrayAdapter<String> activeInfoAdapter;
    //The list view that contains the on-screen info being displayed
    private ListView userListView;
    //ArrayList storing names and numbers in outbox
    private ArrayList<String> activeNums = new ArrayList<>();
    private boolean receiverRegistered = false;
    public static boolean appIsInForeground = false;

    smsBroadcastReceiver onNewMsg = new smsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateActiveNums(intent.getStringExtra("number"));
        }
    };

    IntentFilter onNewMsgFilter = new IntentFilter("com.bccs.bsecure.msgReceived");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Get ActionBar Item
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//        ActionBar actionBar = getActionBar();

        //initialize global variables
        userListView = (ListView) findViewById(R.id.usersListView);
        //Define Listener method
        userListView.setOnItemClickListener(onListClick);
        //Read outbox for active conversation information
        //Initialize the adapter to hold the names of active contacts
        activeInfoAdapter = new ArrayAdapter<String>
                (getApplicationContext(), R.layout.row_layout, activeNums);
        //Display the names.
        updateActiveNums();
        userListView.setAdapter(activeInfoAdapter);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
        receiverRegistered = true;
        appIsInForeground = true;
    }
    private AdapterView.OnItemClickListener onListClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            Intent conversationIntent = new Intent(getApplicationContext(), Conversation.class);
            conversationIntent.putExtra("name", activeNums.get(pos));
            conversationIntent.putExtra("number", activeNums.get(pos));
            startActivity(conversationIntent);
        }
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
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
                return true;
        }
    }
    public void openNewMessage(){
        Intent intent = new Intent(this, CreateMessage.class);
        startActivity(intent);
    }
    public void openContacts(){
        Intent intent = new Intent(this, Contacts.class);
        startActivity(intent);
    }
    public void openNFC(){
        Intent intent = new Intent(this, NFC.class);
        startActivity(intent);
    }

    public void openNoNFC() {
        Intent intent = new Intent(this, NoNFC.class);
        startActivity(intent);
    }
    public void openSettings(){
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }
    public void openBugReport(){
        Intent intent = new Intent(this, BugReport.class);
        startActivity(intent);
    }
    public void openAbout(){
        Intent intent = new Intent(this, About.class);
        startActivity(intent);
    }
    protected void onStart() {
        System.out.println("Convo view onStart");
        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiverRegistered = true;
        }
        updateActiveNums();
        appIsInForeground = true;
        super.onStart();
    }


    private void updateActiveNums(String newNumber) {
        dbHelper appHelper = new dbHelper(this);
        ArrayList<String> newNums = appHelper.getActiveNumbers();
        if (!activeNums.contains(newNumber)) activeNums.add(newNumber);
        for (String s : newNums) {
            if (!activeNums.contains(s)) {
                activeNums.add(s);
            }
        }
        appHelper.close();
        activeInfoAdapter.notifyDataSetChanged();
    }

    private void updateActiveNums() {
        dbHelper appHelper = new dbHelper(this); //init DB access
        // TODO: Better method for retrieving active numbers that incorporates contact list names
        ArrayList<String> newNums = appHelper.getActiveNumbers(); //pull all numbers from DB
        for (String s : newNums) {
            if (!activeNums.contains(s)) {
                activeNums.add(s);
            }
        }
        appHelper.close();
        activeInfoAdapter.notifyDataSetChanged();
    }
    protected void onResume() {
        System.out.println("Convo view onResume");
        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiverRegistered = true;
        }
        updateActiveNums();
        appIsInForeground = true;
        super.onResume();
    }

    protected void onPause() {
        System.out.println("Convo view onPause");
        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
        super.onPause();
    }

    protected void onStop() {
        System.out.println("Convo view onStop");
        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
        super.onStop();
    }


    protected void onDestroy() {
        System.out.println("Convo view onDestroy");
        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
        appIsInForeground = false;
        super.onDestroy();
    }
}
