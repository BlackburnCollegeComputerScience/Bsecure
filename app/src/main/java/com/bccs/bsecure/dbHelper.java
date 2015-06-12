package com.bccs.bsecure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by traci.kamp on 11/11/2014.
 * This file serves as the creator and traverses
 * the outbox database required by this
 * application.
 *
 * Modified by traci.kamp on 11/17/2014.
 * Added getConversation() method to read the outbox
 * for all messages from a specified phone number
 * for display in the conversation history.
 *
 * Modified by traci.kamp on 12/1/2014.
 * Removed system.outs used for debugging. Some
 * bugs remain and need to be resolved. Why
 * doesn't the emulator show up as a name in
 * the active conversations list?
 *
 * Modified by lucas.burdell 6/5/2015.
 * Restructured table to reflect changes
 * made in myMessage object. This database helper will
 * most likely only ever be used for storing and
 * retrieving messages. Should messages be encrypted
 * when stored?
 *
 *
 *
 */

public class dbHelper extends SQLiteOpenHelper {
    public static final String TABLE_MESSAGES = "messages";
    //Table Info
    public static final String COLUMN_ID = "id"; //ID of message
    public static final String COLUMN_SEND_TO_NUM = "send_to_num"; // number sent to / received from
    public static final String COLUMN_BODY = "msg_body"; // body of the message
    public static final String COLUMN_SENT = "sent"; // whether it was sent or received
    public static final String COLUMN_TIME = "msg_time"; // time message sent / received
    //Database Info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Msgs.db";

    //Temporary Diffie-Hellman key storage
    //TODO: Pull this out of here and put it in Dr Coogan's contact list database
    public static final String TABLE_CONTACTS = "contacts";
    public static final String COLUMN_NUM = "contact_num";
    public static final String COLUMN_KEY = "contact_key";
    public static final String COLUMN_CONTACT_ID = "contact_id";

    public dbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //table creation...
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_SEND_TO_NUM + " TEXT," + COLUMN_BODY + " TEXT," + COLUMN_SENT + " INTEGER," +
                COLUMN_TIME + " INTEGER" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "(" +
                COLUMN_CONTACT_ID + " INTEGER PRIMARY KEY," + COLUMN_NUM +
                " TEXT," + COLUMN_KEY  + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    //database version upgrade... destroys old and recreates
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    //adds single record
    public void addRecord(myMessage msg) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        // Fill vals with appropriate content
        vals.put(COLUMN_SEND_TO_NUM, msg.get_number());
        vals.put(COLUMN_BODY, msg.getBody());
        vals.put(COLUMN_SENT, msg.getSent() ? 1 : 0);
        vals.put(COLUMN_TIME, msg.get_time());
        // Insert
        dbase.insert(TABLE_MESSAGES, null, vals);
        Log.i("Adding record: ", msg.get_name() + " " + msg.get_number());
        dbase.close();
    }

    public void addKey(String number, String key) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(COLUMN_NUM, number);
        vals.put(COLUMN_KEY, key);
        dbase.insert(TABLE_CONTACTS, null, vals);
        Log.i("Adding key: ", number + " " + key);
        dbase.close();
    }

    public String getKey(String number) {
        String select = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + COLUMN_NUM + " = " + number;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor c = dbase.rawQuery(select, null);
        if (c != null) {
            c.moveToFirst();
            return c.getString(2);
        } else {
            return null;
        }
    }

    //need to find a way to search through and retrieve unique information from msg items
    //these methods go here

    //get single message
    public myMessage getSingleMessage(int id) {

        String select = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_ID + " = " + id;
        SQLiteDatabase dbase = this.getReadableDatabase();
        Cursor c = dbase.rawQuery(select, null);
        if (c != null) {
            c.moveToFirst();
        }

        myMessage retObj = new myMessage(c.getString(1), c.getString(2), c.getInt(3) == 1);
        retObj.setId(Integer.parseInt(c.getString(0)));
        retObj.set_time(c.getInt(4));
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
            System.out.println(pNum);
            //System.out.println(cName);
            if (!activeInfo.contains(pNum)) {
                activeInfo.add(pNum);
                //names.add(cName);
            } else {

            }

            //if(!names.contains(cName)) names.add(cName);
        }
        //activeInfo[0] = nums;
        //activeInfo[1] = names;

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

    public ArrayList<myMessage> getConversationMessages(String no) {
        ArrayList<myMessage> ret = new ArrayList<>();
        for (int i = 1; i <= this.getRecordCount(); i++) {
            if (this.getSingleMessage(i).get_number().equalsIgnoreCase(no)) {
                ret.add(this.getSingleMessage(i));
            }
        }
        return ret;
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
    public void deleteRecord(myMessage msg) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        dbase.delete(TABLE_MESSAGES, COLUMN_ID + " = ?",
                new String[]{String.valueOf(msg.getId())});
        dbase.close();
    }
}
