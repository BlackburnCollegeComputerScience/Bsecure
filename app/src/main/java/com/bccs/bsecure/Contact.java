package com.bccs.bsecure;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Created by lucas.burdell on 6/24/2015.
 */
public class Contact {
    private long id;         //this should be primary _ID for contact from android db
    private String name;
    private String number;
    private String lookupKey;
    private static Context baseContext = null;


    public Contact() { };

    public Contact(long id) {
        this(baseContext, id);
    }

    public Contact(Context context, long id) {
        this.setId(id);

        if (baseContext == null) baseContext = context.getApplicationContext();
        loadFromAndroidDB();
    }

    public void loadFromAndroidDB() {
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String CONTACT_KEY  = ContactsContract.Contacts.LOOKUP_KEY;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String[] contact_projection = {
                _ID,
                CONTACT_KEY,
                DISPLAY_NAME,
                HAS_PHONE_NUMBER
        };

        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        String[] phone_projection = {
                Phone_CONTACT_ID,
                NUMBER
        };

        ContentResolver contentResolver = baseContext.getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, contact_projection, _ID + "=" + this.getId(), null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            this.setName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
            this.setLookupKey(cursor.getString(cursor.getColumnIndex(CONTACT_KEY)));
            int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));

            if (hasPhoneNumber > 0) {
                Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, phone_projection, Phone_CONTACT_ID + " = ?", new String[]{Long.toString(this.getId())}, null);
                phoneCursor.moveToFirst();
                this.setNumber(phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER)));
                phoneCursor.close();
            }
        }
        cursor.close();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {

        if (id==0) throw new RuntimeException("HEY YOU CAN'T DO THAT");
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public static Context getBaseContext() {
        return baseContext;
    }

    public static void setBaseContext(Context context) {
        baseContext = context;
    }

    public static long getIdFromNumber(String number) {
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String[] contact_projection = {
                _ID,
                DISPLAY_NAME,
                HAS_PHONE_NUMBER
        };

        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        String[] phone_projection = {
                Phone_CONTACT_ID,
                NUMBER
        };

        ContentResolver contentResolver = baseContext.getContentResolver();
        //Cursor cursor = contentResolver.query(PhoneCONTENT_URI, phone_projection, NUMBER + "=?", new String[]{"\"" + number + "\""}, null);
        Cursor cursor = contentResolver.query(PhoneCONTENT_URI, phone_projection, null, null, null);
        long output = -1;

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {

                String thisNumber = cursor.getString(cursor.getColumnIndex(NUMBER));

                if (thisNumber.equals(number)) {

                    output = Long.parseLong(cursor.getString(cursor.getColumnIndex(Phone_CONTACT_ID)));
                }
            }
        } else {
            throw new RuntimeException("No matching IDs");
        }

        cursor.close();
        return output;
    }

    @Override
    public String toString() {
        return "Name:" + this.getName() + "\nID:" + this.getId() + "\nNumber:" + this.getNumber();
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }
}
