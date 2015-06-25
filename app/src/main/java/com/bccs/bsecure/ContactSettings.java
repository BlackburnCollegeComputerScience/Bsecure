package com.bccs.bsecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

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

public class ContactSettings extends ActionBarActivity {

    SecurityContact contact;

    //Layout Objects
    TextView nameTv = (TextView) findViewById(R.id.nameTv);
    TextView androidIdTv = (TextView) findViewById(R.id.androidIdTv);
    TextView sequenceNumberTv = (TextView) findViewById(R.id.sequanceNumberTv);
    TextView expirationTv = (TextView) findViewById(R.id.experationTv);
    TextView remainingKeysTv = (TextView) findViewById(R.id.remainingKeysTv);
    Button exchangeBtn = (Button) findViewById(R.id.exchangeBtn);
    Button forceExpirationBtn = (Button) findViewById(R.id.forceExpBtn);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Grab the security contact from the bundle
        byte[] serializedContact = savedInstanceState.getByteArray("contact");
        try {
            //De-serialized contact object
            contact = deserialize(serializedContact);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //Set up settings display
        nameTv.setText(contact.getName());
        androidIdTv.setText(contact.getID());
        sequenceNumberTv.setText(contact.getSeqNum());
        expirationTv.setText(getKeyExpiration());
        remainingKeysTv.setText(getRemainingKeys());
    }

    private String getRemainingKeys() {
        //TODO
        return "";
    }

    private String getKeyExpiration() {
        //TODO: Calculate key expiration
        return "";
    }

    public static SecurityContact deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return (SecurityContact) o.readObject();
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
