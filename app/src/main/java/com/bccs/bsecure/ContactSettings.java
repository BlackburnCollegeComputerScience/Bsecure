package com.bccs.bsecure;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
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
    TextView nameTv;
    TextView androidIdTv;
    TextView sequenceNumberTv;
    TextView expirationTv;
    TextView remainingKeysTv;
    Button exchangeBtn;
    Button forceExpirationBtn;
    Button forceAllKeyExperationBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_settings);

        nameTv = (TextView) findViewById(R.id.nameTv);
        androidIdTv = (TextView) findViewById(R.id.androidIdTv);
        sequenceNumberTv = (TextView) findViewById(R.id.sequanceNumberTv);
        expirationTv = (TextView) findViewById(R.id.experationTv);
        remainingKeysTv = (TextView) findViewById(R.id.remainingKeysTv);
        exchangeBtn = (Button) findViewById(R.id.exchangeBtn);
        forceExpirationBtn = (Button) findViewById(R.id.forceExpBtn);
        forceAllKeyExperationBtn = (Button) findViewById(R.id.ExpireAllBtn);
        Button addFakeKeys = (Button) findViewById(R.id.addFakeKeys);

        contact = new SecurityContact(getIntent().getExtras().getLong("contact"));

        //Set up settings display
        nameTv.setText(contact.getName());
        androidIdTv.setText("Android ID: " + contact.getId());
        sequenceNumberTv.setText("Sequence Number: " + contact.getSeqNum());
        expirationTv.setText("Messages Till Key Expiration: " + contact.getUsesLeft());
        remainingKeysTv.setText("Remaining Keys: " + contact.getRemainingKeys());

        exchangeBtn.setText("Exchange keys with " + contact.getName());
        exchangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {
                    Intent intent = new Intent(ContactSettings.this, Bluetooth.class);
                    startActivityForResult(intent, Constants.REQUEST_KEYS);
                } else {
                    Intent intent = new Intent(ContactSettings.this, SmsBroadcastReceiver.class);
                    startActivity(intent);
                }

            }
        });


        forceExpirationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ContactSettings.this);
                builder.setTitle("Warning!");
                builder.setMessage("This action is irreversible! The current key in use will be deleted. Do you want to continue?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        contact.expireCurrentKey();
                    }
                });
                //Negative button is null because we just want to cancel the dialog not perform an action
                builder.setNegativeButton("No", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        forceAllKeyExperationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ContactSettings.this);
                builder.setTitle("Warning!");
                builder.setMessage("This action is irreversible! All keys will be deleted. Do you want to continue?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        contact.expireAllKeys();
                    }
                });
                //Negative button is null because we just want to cancel the dialog not perform an action
                builder.setNegativeButton("No", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        addFakeKeys.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] keys = new String[] {
                        "clAycpqr1ciK3ZoBXtYaNveeDXwU/OZDixy2HcPw8gI=",
                        "+VotjZgpHO24pJ0jZDTrkJtC9KL4hAImCI/wxfjTtpg=",
                        "N8UILlMeG3zLQxypCNvbP0Vtpwkj2aBiWfIvd/LZrQk=",
                        "94t7nwZ9QB0RW3ujP66N9acrYylyQ2lD6TDmDxPS214=",
                        "WnnjnvM0vIKDaCLqYBTBYgATAriAY2aj2K6/Bu0rn2A="
                };

                String[] ivs = new String[]{
                        "SftCfKsoT7PUjn4HsmmBmA==",
                        "a6qFpB1HK/UUGV5Qw1Tp1w==",
                        "0K5Hi1PDZ/ZtyymZPDGhew==",
                        "qARRXPOoLH9MEZqBvNf/Yg==",
                        "NAUi58khEiU307IpgGE83Q=="
                };

                contact.addKeys(keys, ivs);
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
                String[] ivs = data.getExtras().getStringArray("ivs");
                int expireCount = data.getExtras().getInt("expireCount");
                contact.addKeys(keys, ivs);
                contact.setUses(expireCount);
            }
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
