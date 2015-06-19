package com.bccs.bsecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

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

public class CreateMessage extends ActionBarActivity {
    //Global Variables
    ListView recipientList;

    //Array of strings that the recipientList is drawn from
    ArrayList<String> recipientStrings = new ArrayList<String>();
    //String adapter that will handle the data of the recipientList
    ArrayAdapter<String> adapter;

    //Buttons
    Button addContactButton;
    Button sendMessage;
    //TextFields
    EditText addContactText;
    EditText messageText;
    /**
     * This method will send the typed message to all members of the RecipientList
     */
    private Button.OnClickListener sendMessageListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            for (String number : recipientStrings) {
                dbHelper database = new dbHelper(getApplicationContext());
                database.addRecord(handleMessage.send(number, messageText.getText().toString(), getApplicationContext()));
                database.close();
            }
            Intent conversationIntent = new Intent(getApplicationContext(), Conversation.class);
            conversationIntent.putExtra("name", recipientStrings.get(0));
            conversationIntent.putExtra("number", recipientStrings.get(0));
            startActivity(conversationIntent);
        }
    };
    /**
     * This Method will remove a contact that has been clicked on the recipientList
     */
    private AdapterView.OnItemClickListener onListClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            recipientStrings.remove(pos);
            adapter.notifyDataSetChanged();
        }
    };
    /**
     * This Method will take the Recipient the user entered and add it to the contact list
     */
    private Button.OnClickListener addRecipientToList = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            recipientStrings.add(addContactText.getText().toString());
            adapter.notifyDataSetChanged();
            addContactText.setText("");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
        //Initialize Global Variables
        addContactButton = (Button) findViewById(R.id.addRecipientButton);
        sendMessage = (Button) findViewById(R.id.sendButton);
        addContactText = (EditText) findViewById(R.id.recipientEditText);
        messageText = (EditText) findViewById(R.id.messageEditText);
        //Performs recipientList setup
        setupRecipientList();
        //Adds a listener to the addContactButton
        addContactButton.setOnClickListener(addRecipientToList);
        //Adds a listener to the handleMessage Button
        sendMessage.setOnClickListener(sendMessageListener);
    }

    /**
     * This will apply all necessary settings to the recipientList
     */
    private void setupRecipientList() {
        //Grab the recipientList object
        recipientList = (ListView) findViewById(R.id.recipientList);
        //Adds the click listener for the recipientList
        recipientList.setOnItemClickListener(onListClick);
        //Sets up the recipientList adapter
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                recipientStrings);
        recipientList.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_message, menu);
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
