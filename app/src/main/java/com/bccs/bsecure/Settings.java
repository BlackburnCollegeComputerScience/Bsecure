package com.bccs.bsecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

    SCSQLiteHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        minimumEt = (EditText) findViewById(R.id.MinimumEt);
        maximumEt = (EditText) findViewById(R.id.MinimumEt);

        minimumDisplayBtn = (Button) findViewById(R.id.MinimumDisplayBtn);
        maximumDisplayBtn = (Button) findViewById(R.id.MaximumDisplayBtn);
        saveBtn = (Button) findViewById(R.id.SaveBtn);

        database = new SCSQLiteHelper(this);

        int[] settings = database.getGeneralSettings();
        minimumDisplayBtn.setText(settings[0]);
        maximumDisplayBtn.setText(settings[1]);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int min = Integer.parseInt(minimumEt.getText().toString());
                int max = Integer.parseInt(maximumEt.getText().toString());
                if (min > max) {
                    min = max;
                    minimumEt.setText(min);
                }
                minimumDisplayBtn.setText(min);
                maximumDisplayBtn.setText(max);
                database.setGeneralSettings(min, max);
            }
        });


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
