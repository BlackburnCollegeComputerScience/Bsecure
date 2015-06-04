package com.bccs.bsecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


public class Main extends AppCompatActivity {

    //Global class variables

    //The SQLite DB helper - traverses outbox
    public dbHelper appHelper;
    private boolean dbActive = false;
    //The adapter to use when displaying to the screen.
    private ArrayAdapter<String> activeInfoAdapter;
    //The list view that contains the on-screen info being displayed
    private ListView userListView;
    //ArrayList storing names and numbers in outbox
    private ArrayList<String>[] activeNums;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Get ActionBar Item
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//        ActionBar actionBar = getActionBar();
        appHelper = new dbHelper(this);
        dbActive = true;
        //initialize global variables
        userListView = (ListView) findViewById(R.id.usersListView);
        //Define Listener method
        userListView.setOnItemClickListener(onListClick);
        //Read outbox for active conversation information
        activeNums = appHelper.getActiveNumbers();
        //Initialize the adapter to hold the names of active contacts
        activeInfoAdapter = new ArrayAdapter<String>
                (getApplicationContext(), R.layout.row_layout, activeNums[1]);
        //Display the names.
        userListView.setAdapter(activeInfoAdapter);
    }
    private AdapterView.OnItemClickListener onListClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            Intent conversationIntent = new Intent(getApplicationContext(), messageSender.class);
            conversationIntent.putExtra("name", activeNums[1].get(pos));
            conversationIntent.putExtra("number", activeNums[0].get(pos));
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
                openNFC();
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
//                onBackPressed();
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
        if (!dbActive) {
            appHelper = new dbHelper(this);
            dbActive = true;
        }
        super.onStart();
    }

    protected void onResume() {
        System.out.println("Convo view onResume");
        if (!dbActive) {
            appHelper = new dbHelper(this);
            dbActive = true;
        }
        super.onResume();
    }

    protected void onPause() {
        System.out.println("Convo view onPause");
        appHelper.close();
        dbActive = false;
        super.onPause();
    }
}
