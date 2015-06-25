package com.bccs.bsecure;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        setContentView(R.layout.activity_contact_settings);

        //Grab the security contact from the bundle
        byte[] serializedContact = getIntent().getExtras().getByteArray("contact");
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
        androidIdTv.setText("Android ID: " + contact.getId());
        sequenceNumberTv.setText("Sequence Number: " + contact.getSeqNum());
        expirationTv.setText("Messages Till Key Expiration" + getKeyExpiration());
        remainingKeysTv.setText("Remaining Keys: " + getRemainingKeys());

        exchangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {
                    Intent intent = new Intent(ContactSettings.this, Bluetooth.class);
                    startActivityForResult(intent, Constants.REQUEST_KEYS);
                } else {
                    Intent intent = new Intent(ContactSettings.this, smsBroadcastReceiver.class);
                    startActivity(intent);
                }

            }
        });

        forceExpirationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Create a protocol for informing other party of key expiration
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //onActivityResult is called when a intent finished and comes back to this activity.
        if (requestCode == Constants.REQUEST_KEYS) {
            //If we requested a key exchange
            if (resultCode == RESULT_OK) {
                //Grab the keys from the data packet
                String[] keys = data.getExtras().getStringArray("keys");
                int expireCount = data.getExtras().getInt("expireCount");
                contact.addKeys(keys);
                contact.setUses(expireCount);
            }
        }
    }

    private String getRemainingKeys() {
        //TODO
        return "";
    }

    private String getKeyExpiration() {
        return contact.getUsesLeft() + "";
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
        onBackPressed();
        return true;
    }
}
