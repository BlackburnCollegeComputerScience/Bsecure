package com.bccs.bsecure;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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

public class Contacts extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Button test = (Button) findViewById(R.id.testButton);
        test.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
                selectContact();
            }
        });
    }

    static final int REQUEST_SELECT_PHONE_NUMBER = 1;

    public void selectContact() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_PHONE_NUMBER);
        }
    }

    private static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_PHONE_NUMBER && resultCode == RESULT_OK) {
            // Get the URI and query the content provider for the phone number
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = getContentResolver().query(contactUri, projection,
                    null, null, null);
            // If the cursor returned is valid, get the phone number
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(numberIndex);
                System.out.println(number);
                int IDIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                System.out.println(IDIndex);
                // Do something with the phone number
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
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
