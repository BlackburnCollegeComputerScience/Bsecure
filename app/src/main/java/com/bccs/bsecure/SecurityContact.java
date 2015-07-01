package com.bccs.bsecure;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;

/*
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 * <p>
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 */

public class SecurityContact extends Contact {
//    private int id;         //this should be primary _ID for contact from android db
//    private String name;
//    private String number;
    private int seqNum;
    private int seqMax;
    private String currKey = null;
    private String currIV = null;
    private int totalKeys = 1000;
    private int usesLeft = 100;
    private int timeLeft = 0;
    private int usesMax = 100;
    private static SCSQLiteHelper database = null;

    /* need empty constructor */

    public SecurityContact() {
    }

    public SecurityContact(long id) {
        this(Contact.getBaseContext(), id);
    }

    public SecurityContact(Context context, long id) {
        super(context, id);

        //Initialize conversation
        //this.number should have been loaded by loadFromAndroidDB


        //Lookup in SC db.scentry table using id
        //if exists, BSecure contact exists, use db values
        //else, BSecure contact does not exist, create with default values

        if (database == null) database = new SCSQLiteHelper(context);
        if (database.contactIsInDatabase(id)) {
            this.seqNum = database.getContactSeqNum(id);
            this.seqMax = database.getContactSeqMax(id);
            this.totalKeys = database.getContactTotalKeys(id);
            System.out.println("Total keys loaded: " + this.totalKeys);
            this.timeLeft = database.getContactTimeLeft(id);
            this.usesLeft = database.getContactUsesLeft(id);
            this.usesMax = database.getContactUsesMax(id);


            //Lookup in SC db.keypairentry table using id and seqnum
            //if does not exist, no more key pairs available. Throw exception??
            //this.currKey = "0123ABC";
            this.currKey = database.getKey(getId(), this.seqNum);
            this.currIV = database.getIV(getId(), this.seqNum);

        } else {
            this.seqNum = -1;
            this.seqMax = -1;
            this.totalKeys = 1000;
            this.timeLeft = 0;
            this.usesLeft = 0;
            this.usesMax = 100;
            this.currKey = null;
            this.currIV = null;
            database.createSecurityContact(this);
        }
    }

    public String getSessionIV() {
        if (this.seqNum < 0 && this.seqMax == -1) return null;
        return getIV();
    }

    public String getSessionKey() {
        System.out.println("Getting session key!");
        if (this.seqNum < 0 && this.seqMax == -1) return null;
        if (keyExpired() || this.seqNum < 0) {
            currKey = getNextKey();      //lookup in SC db.keypairentry using id and seqnum
        }
        this.usesLeft--;
        database.setContactUsesLeft(this.getId(), this.usesLeft);
        return getKey();
    }

    private String getIV() {
        return this.currIV;
    }

    private String getKey() {
        return currKey;
    }

    public void receivedExpireAllKeys() {
        database.clearKeys(this.getId());
        this.seqNum = -1;
        this.seqMax = -1;
        this.usesLeft = 0;
        this.save();
    }

    public void receivedExpireKey() {
        getNextKey();
    }

    public void expireAllKeys() {
        HandleMessage.sendAllkeysExpired(this);
        receivedExpireAllKeys();
    }

    public void expireCurrentKey() {
        HandleMessage.sendKeyExpired(this);
        receivedExpireKey();
    }

    public int getRemainingKeys() {
        if (seqNum<=seqMax) {
            return seqNum - seqMax + 1;
        } else {
            return totalKeys - seqNum + seqMax + 1;
        }
    }

    public int getSeqNum() {
        return seqNum;
    }


    private boolean keyExpired() {
        System.out.println("Uses left: " + getUsesLeft());
        return this.getUsesLeft() <= 0;
    }

    private String getNextKey() {
        System.out.println("Getting next key");
        if (this.seqNum >= 0) {
            database.clearKey(this.seqNum, this.getId());
        }
        if (this.seqNum == this.getSeqMax()) {
            this.seqNum = -1;
            return null;
        }
        this.seqNum = Math.abs((this.seqNum + 1) % this.getTotalKeys());
        this.usesLeft = this.getUsesMax();
        String key = database.getKey(this.getId(), this.seqNum);
        if (key == null) System.out.println("Key missing - LB");
        save();
        return key;
    }

    public void save() {
        try {
            database.createSecurityContact(this);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }


    public static void close() {
        database.close();
    }

    public void addKeys(String[] keys, String[] IVs) {
        System.out.println("Adding keys!");
        System.out.println("seqMax: " + this.seqMax + " keys: " + keys.length);
        System.out.println("Total keys: " + this.totalKeys);
        int prediction = Math.abs((this.seqMax + keys.length + 1) % this.totalKeys);
        System.out.println("Prediction: " + prediction);
        int newMax = database.addKeys(keys, IVs, this.getId(), this.seqNum, this.seqMax, this.totalKeys);
        System.out.println("newMax: " + newMax);
        if (prediction!=newMax) {
            //TODO: Ran out of room for keys!
        }
        this.seqMax = newMax;
        save();
    }

    public int getSeqMax() {
        return seqMax;
    }

    public int getTotalKeys() {
        return totalKeys;
    }

    public int getUsesLeft() {
        return usesLeft;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public int getUsesMax() {
        return usesMax;
    }

    public void setUses(int uses) {
        this.usesMax = uses;
        save();
    }

    public static ArrayList<Contact> getSecurityContacts(Context context) {
        System.out.println("GetSecurityContacts called");
        ArrayList<Contact> contacts = new ArrayList<>();
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        SCSQLiteHelper database = new SCSQLiteHelper(context);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        if(cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                if (database.contactIsInDatabase(Integer.parseInt(contact_id))) {
                    System.out.println("ID " + contact_id + " added");
                    contacts.add(new Contact(context, Integer.parseInt(contact_id)));
                }
            }
        }
        cursor.close();
        return contacts;
    }

    public static ArrayList<Contact> getNonSecurityContacts(Context context) {
        ArrayList<Contact> contacts = new ArrayList<>();
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        SCSQLiteHelper database = new SCSQLiteHelper(context);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        if(cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                if (!database.contactIsInDatabase(Integer.parseInt(contact_id))) {
                    contacts.add(new Contact(context, Integer.parseInt(contact_id)));
                }
            }
        }
        cursor.close();
        return contacts;
    }


    public static ArrayList<Contact> getAllContacts(Context context) {
        ArrayList<Contact> contacts = new ArrayList<>();
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        if(cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                Contact contact = new Contact(context, Integer.parseInt(contact_id));
                contacts.add(contact);
            }
        }
        cursor.close();
        return contacts;
    }

    public static boolean contactIdIsASecurityContact(long id) {
        SCSQLiteHelper database = new SCSQLiteHelper(Contact.getBaseContext());
        return (database.contactIsInDatabase(id));
    }

    public static void expireAllContactsKeys() {
        ArrayList<Contact> contacts = getSecurityContacts(Contact.getBaseContext());
        ArrayList<SecurityContact> sContacts = new ArrayList<>();
        for (Contact c : contacts) {
            sContacts.add(new SecurityContact(Contact.getBaseContext(), c.getId()));
        }
        for (SecurityContact sc : sContacts) {
            sc.expireAllKeys();
        }
    }

}
