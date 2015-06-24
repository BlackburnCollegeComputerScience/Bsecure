package com.bccs.bsecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


/*
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

public class TestActivity extends ActionBarActivity {

    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
//        SecurityContact lucas = new SecurityContact(100, "Lucas", "15555215554");
//        SecurityContact shane = new SecurityContact(101, "Shane", "15555215556");
//        SecurityContact kevin = new SecurityContact(102, "Kevin", "15555215558");
//        SCSQLiteHelper dbase = new SCSQLiteHelper(this);
//        try {
//            dbase.createSecurityContact(lucas);
//            dbase.createSecurityContact(shane);
//            dbase.createSecurityContact(kevin);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        int id = lucas.getID();
//        String fromID = dbase.getFromID(id);
//        System.out.println(dbase.getFromID(shane.getID()));
//        System.out.println(dbase.getFromID(kevin.getID()));
//
//        dbase.clearDatabase();
//
//        System.out.println(fromID);
//        System.out.println(dbase.getFromID(shane.getID()));
//        System.out.println(dbase.getFromID(kevin.getID()));
//        dbase.close();

        status = (TextView) findViewById(R.id.result);
        Intent intent = new Intent(this, Bluetooth.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //onActivityResult is called when a intent finished and comes back to this activity.
        if (requestCode == 1) {
            //If we requested a key exchange
            if (resultCode == RESULT_OK) {
                //Grab the keys from the data packet
                String[] keys = data.getExtras().getStringArray("keys");
                int expireCount = data.getExtras().getInt("expireCount");

                status.append("Here is how long a key will last: " + expireCount + "\n\n");
                status.append("Here are all the key hash codes:\n");

                for (int i = 0; i < keys.length; i++) {
                    status.append(keys[i].hashCode() + "\n");
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
