package com.bccs.bsecure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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

/**
 * Created by traci.kamp on 11/11/2014.
 * This file serves as the creator and traverses
 * the outbox database required by this
 * application.
 * <p>
 * Modified by traci.kamp on 11/17/2014.
 * Added getConversation() method to read the outbox
 * for all messages from a specified phone number
 * for display in the conversation history.
 * <p>
 * Modified by traci.kamp on 12/1/2014.
 * Removed system.outs used for debugging. Some
 * bugs remain and need to be resolved. Why
 * doesn't the emulator show up as a name in
 * the active conversations list?
 * <p>
 * Modified by lucas.burdell 6/5/2015.
 * Restructured table to reflect changes
 * made in myMessage object. This database helper will
 * most likely only ever be used for storing and
 * retrieving messages. Should messages be encrypted
 * when stored?
 * <p>
 * Modified by lucas.burdell 6/11/2015.
 * Added new table that tracks diffie-hellman pairs.
 * <p>
 * Modified by lucas.burdell 6/12/2015.
 * Added new collumn to messages table to track whether a message was encrypted or not.
 * This is mainly for the conversation UI.
 */

public class DbHelper extends SQLiteOpenHelper {
    public static final String TABLE_MESSAGES = "messages";
    //Table Info
    public static final String COLUMN_ID = "id"; //ID of message
    public static final String COLUMN_SEND_TO_NUM = "send_to_num"; // number sent to / received from
    public static final String COLUMN_BODY = "msg_body"; // body of the message
    public static final String COLUMN_SENT = "sent"; // whether it was sent or received
    public static final String COLUMN_TIME = "msg_time"; // time message sent / received
    public static final String COLUMN_ENC = "msg_encrypted";
    //Database Info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Msgs.db";

    //Temporary Diffie-Hellman key storage

    public static final String TABLE_CONTACTS = "contacts";
    //Table info
    public static final String COLUMN_NUM = "contact_num"; //contact's number
    public static final String COLUMN_KEY = "contact_key"; //secret key
    public static final String COLUMN_HASH = "contact_keyhash"; //hash of secret for IV
    public static final String COLUMN_CONTACT_ID = "contact_id"; //row ID

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //table creation...
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_SEND_TO_NUM + " TEXT," + COLUMN_BODY + " TEXT," + COLUMN_SENT + " INTEGER," +
                COLUMN_TIME + " TEXT," + COLUMN_ENC + " INTEGER" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "(" +
                COLUMN_CONTACT_ID + " INTEGER PRIMARY KEY," + COLUMN_NUM +
                " TEXT," + COLUMN_KEY + " TEXT," + COLUMN_HASH + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    //database version upgrade... destroys old and recreates
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }


    //adds single record
    public void addRecord(MyMessage msg) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        // Fill vals with appropriate content
        vals.put(COLUMN_SEND_TO_NUM, msg.get_number());
        vals.put(COLUMN_BODY, msg.getBody());
        vals.put(COLUMN_SENT, msg.getSent() ? 1 : 0);
        vals.put(COLUMN_TIME, ((Long) msg.get_time()).toString());
        vals.put(COLUMN_ENC, msg.is_encrypted() ? 1 : 0);
        // Insert
        dbase.insert(TABLE_MESSAGES, null, vals);
        Log.i("Adding record: ", msg.get_name() + " " + msg.get_number());
        dbase.close();
    }

    public void addKey(String number, String key, String hash) {

        int id = checkIfKeyPresent(number);
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        if (id <= 0) {
            vals.put(COLUMN_NUM, number);
            vals.put(COLUMN_KEY, key);
            vals.put(COLUMN_HASH, hash);
            vals.put(COLUMN_CONTACT_ID, getKeyCount() + 1);
            dbase.insert(TABLE_CONTACTS, null, vals);
        } else {
            vals.put(COLUMN_KEY, key);
            vals.put(COLUMN_HASH, hash);
            dbase.update(TABLE_CONTACTS, vals, COLUMN_CONTACT_ID + " =" + Integer.toString(id), null);
        }
        Log.i("Adding key: ", number + " " + key);
        dbase.close();
    }

    /**
     * Returns the key associated with the number (or null)
     *
     * @param number
     * @return String key
     */
    public String[] getKey(String number) {
        String[] output = new String[2];
        String select = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + COLUMN_NUM + " = " + number;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor c = dbase.rawQuery(select, null);
        if (c != null) {
            try {
                c.moveToFirst();
                output[0] = c.getString(2);
                output[1] = c.getString(3);
                return output;
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * Checks if a key is already in the database. Returns the ID of row for key or -1.
     *
     * @param number Number to search for
     * @return int row ID or -1
     */
    public int checkIfKeyPresent(String number) {
        String select = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + COLUMN_NUM + " = " + number;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor c = dbase.rawQuery(select, null);
        int ret = -1;
        if (c != null) {
            c.moveToFirst();
            ret = c.getPosition();
        }
        dbase.close();
        return ret;
    }

    //get single message
    public MyMessage getSingleMessage(int id) {

        String select = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_ID + " = " + id;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor c = dbase.rawQuery(select, null);
        if (c != null) {
            c.moveToFirst();
        }

        MyMessage retObj = new MyMessage(c.getInt(1), c.getString(2), c.getInt(3) == 1);
        retObj.setId(Integer.parseInt(c.getString(0)));
        retObj.set_time(Long.parseLong(c.getString(4)));
        retObj.set_encrypted(c.getInt(5) == 1);
        c.close();
        dbase.close();
        return retObj;
    }

    public ArrayList<String> getActiveNumbers() {
        ArrayList<String> activeInfo = new ArrayList<>();
        ArrayList<String> nums = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 1; i < this.getRecordCount(); i++) {
            //get phone number out of message object and add it to array list
            String pNum = this.getSingleMessage(i).get_number();

            //String cName = this.getSingleMessage(i).get_name();
            //this.getId()
            if (!activeInfo.contains(pNum)) {
                activeInfo.add(pNum);
                //names.add(cName);
            }
            //if(!names.contains(cName)) names.add(cName);
        }
        //activeInfo[0] = nums;
        //activeInfo[1] = names;

        return activeInfo;
    }

    public ArrayList<MyMessage> getActiveNumbersAsMyMessages() {
        ArrayList<MyMessage> activeInfo = new ArrayList<>();
        ArrayList<String> nums = new ArrayList<String>();
        int recordCount = this.getRecordCount();
        for (int i = 1; i <= recordCount; i++) {
            //get phone number out of message object and add it to array list
            String pNum = this.getSingleMessage(i).get_number();
            if (!nums.contains(pNum)) {
                activeInfo.add(this.getSingleMessage(i));
                nums.add(pNum);
            }
        }

        return activeInfo;
    }

    public ArrayList<String> getConversation(String no) {
        ArrayList<String> ret = new ArrayList<String>();
        for (int i = 1; i <= this.getRecordCount(); i++) {
            if (this.getSingleMessage(i).get_number().equalsIgnoreCase(no)) {
                ret.add(this.getSingleMessage(i).getBody());
            }
        }
        return ret;
    }

    public ArrayList<MyMessage> getConversationMessages(String no) {
        ArrayList<MyMessage> ret = new ArrayList<>();
        for (int i = 1; i <= this.getRecordCount(); i++) {
            if (this.getSingleMessage(i).get_number().equalsIgnoreCase(no)) {
                ret.add(this.getSingleMessage(i));
            }
        }
        return ret;
    }

    public void clearAllMessages() {
        SQLiteDatabase dbase = this.getWritableDatabase();
        dbase.delete(TABLE_MESSAGES, null, null);
        dbase.close();
    }

    public void clearMessagesFromNumber(String num) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        dbase.delete(TABLE_MESSAGES, COLUMN_SEND_TO_NUM + "=" + num, null);
        dbase.close();
    }

    public int getKeyCount() {
        String countQuery = "SELECT * FROM " + TABLE_CONTACTS;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor cursor = dbase.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    //gets a count of records stored in DB
    public int getRecordCount() {
        String countQuery = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor cursor = dbase.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    //deletes a single record
    public void deleteRecord(MyMessage msg) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        dbase.delete(TABLE_MESSAGES, COLUMN_ID + " = ?",
                new String[]{String.valueOf(msg.getId())});
        dbase.close();
    }
}
