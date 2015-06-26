package com.bccs.bsecure;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 *
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 */

public class Settings extends ActionBarActivity {

    EditText minimumEt;
    EditText maximumEt;

    Button minimumDisplayBtn;
    Button maximumDisplayBtn;
    Button saveBtn;
    Button expireAllKeysBtn;

    SCSQLiteHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        minimumEt = (EditText) findViewById(R.id.MinimumEt);
        maximumEt = (EditText) findViewById(R.id.MaximumEt);

        minimumDisplayBtn = (Button) findViewById(R.id.MinimumDisplayBtn);
        maximumDisplayBtn = (Button) findViewById(R.id.MaximumDisplayBtn);
        saveBtn = (Button) findViewById(R.id.SaveBtn);
        expireAllKeysBtn = (Button) findViewById(R.id.ExpireAllKeysAllContacts);

        database = new SCSQLiteHelper(this);

        int[] settings = database.getGeneralSettings();
        minimumDisplayBtn.setText(String.valueOf(settings[0]));
        maximumDisplayBtn.setText(String.valueOf(settings[1]));

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String minString = minimumEt.getText().toString();
                String maxString = maximumEt.getText().toString();
                int min = minString.equals("") ? Integer.parseInt(minString) : 0;
                int max = maxString.equals("") ? Integer.parseInt(maxString) : 0;
                int[] settings = database.getGeneralSettings();
                if (min == 0 || min > 2000) {
                    min = settings[0];
                } else if (max == 0 || max > 2000) {
                    max = settings[0];
                }
                if (min > max) {
                    min = max;
                    minimumEt.setText(min);
                }
                minimumDisplayBtn.setText(String.valueOf(min));
                maximumDisplayBtn.setText(String.valueOf(max));
                database.setGeneralSettings(min, max);
                showToast("Settings Saved");
            }
        });

        expireAllKeysBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setTitle("Warning!");
                builder.setMessage("This action is irreversible! All keys for all contacts will be deleted. Do you want to continue?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO: Create a protocol for informing all contacts with keys to be deleted
                    }
                });
                //Negative button is null because we just want to cancel the dialog not perform an action
                builder.setNegativeButton("No", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    private void showToast(String message) {
        //Shortcut method to display a toast
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
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
