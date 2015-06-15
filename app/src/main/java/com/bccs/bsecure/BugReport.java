package com.bccs.bsecure;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class BugReport extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bug_report);
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
                openMain();
                return true;
        }
    }

    public void openMain() {
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
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

}
