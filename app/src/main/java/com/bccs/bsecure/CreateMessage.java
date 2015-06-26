package com.bccs.bsecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    ArrayList<Contact> recipientContacts = new ArrayList<>();



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
            for (Contact c : recipientContacts) {
                ConversationManager.ConversationHelper helper = ConversationManager.getConversation(
                        CreateMessage.this, c.getId());
                helper.addMessage(HandleMessagexxxx.send(c.getId(), messageText.getText().toString(),
                        getApplicationContext()));
            }
            Intent conversationIntent = new Intent(getApplicationContext(), Conversation.class);
            conversationIntent.putExtra("contactid", recipientContacts.get(0).getId());
            startActivity(conversationIntent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
        //Initialize Global Variables
        sendMessage = (Button) findViewById(R.id.sendButton);
        addContactText = (EditText) findViewById(R.id.recipientEditText);
        messageText = (EditText) findViewById(R.id.messageEditText);
        //Performs recipientList setup
        setupRecipientList();
        //Adds a listener to the handleMessage Button
        sendMessage.setOnClickListener(sendMessageListener);
    }

    /**
     * This will apply all necessary settings to the recipientList
     */
    private void setupRecipientList() {

        recipientList = (ListView) findViewById(R.id.recipientList);
        contactSelectionAdapter adapter = new contactSelectionAdapter();
        adapter.addItems(SecurityContact.getAllContacts(this.getApplicationContext()));
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
        onBackPressed();
        return true;
    }
    public void updateRecipientContacts(Contact contact, boolean add) {
        if (add) {
            if (!recipientContacts.contains(contact)) recipientContacts.add(contact);
        } else {
            recipientContacts.remove(contact);
        }
        StringBuffer s = new StringBuffer();
        for (Contact c : recipientContacts) {
            s.append(c.getName());
            s.append(";");
        }
        s.trimToSize();
        addContactText.setText(s);
    }

    private class contactSelectionAdapter extends BaseAdapter {

        private class ContactWrapper {
            private boolean selected;
            private Contact contact;

            public ContactWrapper(Contact contact) {
                this.contact = contact;
                this.selected = false;
            }

            public boolean isSelected() {
                return selected;
            }

            public void setSelected(boolean selected) {
                this.selected = selected;
            }

            public Contact getContact() {
                return contact;
            }
        }

        private static final int MAX_TYPES = 1;

        private ArrayList<ContactWrapper> contactsArray = new ArrayList<>();
        private LayoutInflater inflater;

        public contactSelectionAdapter() {
            //create inflater that will hold the chat boxes
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(Contact contact) {
            contactsArray.add(new ContactWrapper(contact));
            notifyDataSetChanged();
        }

        public void addItems(ArrayList<Contact> contacts) {
            for (Contact c : contacts) {
                addItem(c);
            }
        }

        public ArrayList<Contact> getSelectedContacts() {
            ArrayList<Contact> contacts = new ArrayList<>();
            for (ContactWrapper cw : contactsArray) {
                if (cw.isSelected()) contacts.add(cw.getContact());
            }
            return contacts;
        }

        public void clearItems() {
            contactsArray = new ArrayList<>();
            notifyDataSetChanged();
        }

        public ArrayList<Contact> getContactsArray() {
            ArrayList<Contact> newContact = new ArrayList<>();
            for (ContactWrapper c : contactsArray) {
                newContact.add(c.getContact());
            }
            return newContact;
        }


        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return MAX_TYPES;
        }

        @Override
        public int getCount() {
            return contactsArray.size();
        }

        @Override
        public ContactWrapper getItem(int position) {
            return contactsArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout holder = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    default:
                        convertView = inflater.inflate(R.layout.new_message_contact_row, null);
                        holder = (RelativeLayout) convertView;
                        break;
                }
            } else {
                holder = (RelativeLayout) convertView;
            }
            TextView contactName = (TextView) convertView.findViewById(R.id.contactText);
            TextView contactNumber = (TextView) convertView.findViewById(R.id.numberText);
            final ContactWrapper wrapper = getItem(position);
            final Contact contact = wrapper.getContact();
            contactName.setText(contact.getName());
            contactNumber.setText(contact.getNumber());
            final CheckBox box = (CheckBox) convertView.findViewById(R.id.checkBox);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    box.setChecked(!box.isChecked());
                    wrapper.setSelected(box.isChecked());
                    updateRecipientContacts(contact, wrapper.isSelected());
                }
            });

            return convertView;
        }
    }
}
