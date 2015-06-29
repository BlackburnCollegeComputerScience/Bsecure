package com.bccs.bsecure;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
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

public class Contacts extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        ListView contactsView = (ListView) findViewById(R.id.contactsView);
        contactSelectionAdapter adapter = new contactSelectionAdapter();
        adapter.addItems(SecurityContact.getSecurityContacts(this.getApplicationContext()));
        contactsView.setAdapter(adapter);
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

                int IDIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

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
        switch (id) {
            case R.id.action_new:
                newContact();
                return true;
            default:
                onBackPressed();
                return true;
        }
    }

    private void newContact() {
        Intent intent = new Intent(this, NewContact.class);
        startActivity(intent);
    }
    private class contactSelectionAdapter extends BaseAdapter {


        private static final int MAX_TYPES = 1;

        private ArrayList<Contact> contactsArray = new ArrayList<>();
        private LayoutInflater inflater;

        public contactSelectionAdapter() {
            //create inflater that will hold the chat boxes
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(Contact contact) {
            contactsArray.add(contact);
            notifyDataSetChanged();
        }

        public void addItems(ArrayList<Contact> contacts) {
            for (Contact c : contacts) {
                addItem(c);
            }
        }

        public void clearItems() {
            contactsArray = new ArrayList<>();
            notifyDataSetChanged();
        }

        public ArrayList<Contact> getContactsArray() {
            ArrayList<Contact> newContact = new ArrayList<>();
            for (Contact c : contactsArray) {
                newContact.add(c);
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
        public Contact getItem(int position) {
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
                        convertView = inflater.inflate(R.layout.contact_row, null);
                        holder = (RelativeLayout) convertView;
                        break;
                }
            } else {
                holder = (RelativeLayout) convertView;
            }
            TextView contactName = (TextView) convertView.findViewById(R.id.contactText);
            TextView contactNumber = (TextView) convertView.findViewById(R.id.numberText);
            final Contact contact = getItem(position);
            contactName.setText(contact.getName());
            contactNumber.setText(contact.getNumber());

            Button settingsBtn = (Button) convertView.findViewById(R.id.settingsButton);
            Button editButton = (Button) convertView.findViewById(R.id.editButton);

            editButton.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    Uri contactUri = ContactsContract.Contacts.getLookupUri((long) contact.getId(), contact.getLookupKey());
                    intent.setData(contactUri);
                    intent.putExtra("finishActivityOnSaveCompleted", true);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });

            settingsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Contacts.this, ContactSettings.class);
                    try {
                        intent.putExtra("contact", contact.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }


}
