package com.bccs.bsecure;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class NewContact extends ActionBarActivity {


    contactSelectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_contact);
        adapter = new contactSelectionAdapter();
        Button doneButton = (Button) findViewById(R.id.doneButton);
        ListView contactList = (ListView) findViewById(R.id.contactListView);
        contactList.setAdapter(adapter);
        adapter.addItems(SecurityContact.getNonSecurityContacts(this));

        doneButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NewContact.this);
                builder.setTitle("Add contacts")
                        .setMessage("All selected contacts will be associated with the BSecure app." +
                                "Are you sure?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                               ArrayList<Contact> contactsToAdd = adapter.getSelectedContacts();
                                for (Contact c : contactsToAdd) {
                                    SecurityContact sc = new SecurityContact(NewContact.this, c.getId());
                                    System.out.println("TOTAL KEYS WHEN CREATING: " + sc.getTotalKeys());
                                }
                                dialog.dismiss();
                                onBackPressed();
                                finish();
                            }

                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                onBackPressed();
                                finish();
                            }
                        });

                // Create the AlertDialog object and return it
                Dialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
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
                    }
                });

                return convertView;
            }
        }
}
