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
 * <p/>
 * Modified by traci.kamp on 11/17/2014.
 * Added getConversation() method to read the outbox
 * for all messages from a specified phone number
 * for display in the conversation history.
 * <p/>
 * Modified by traci.kamp on 12/1/2014.
 * Removed system.outs used for debugging. Some
 * bugs remain and need to be resolved. Why
 * doesn't the emulator show up as a name in
 * the active conversations list?
 */

public class dbHelper extends SQLiteOpenHelper {
    public static final String TABLE_MESSAGES = "messages";
    //Table Info
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SEND_TO_NAME = "send_to_name";
    public static final String COLUMN_SEND_TO_NUM = "send_to_num";
    public static final String COLUMN_BODY = "msg_body";
    //Database Info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "outgoingMsgs.db";

    public dbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //table creation...
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_SEND_TO_NAME + " TEXT," +
                COLUMN_SEND_TO_NUM + " TEXT," + COLUMN_BODY + " TEXT" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
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
        vals.put(COLUMN_SEND_TO_NAME, msg.get_name());
        vals.put(COLUMN_SEND_TO_NUM, msg.get_number());
        vals.put(COLUMN_BODY, msg.getBody());
        // Insert
        dbase.insert(TABLE_MESSAGES, null, vals);
        Log.i("Adding record: ", msg.get_name() + " " + msg.get_number());
        dbase.close();
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

        myMessage retObj = new myMessage(Integer.parseInt(c.getString(0)),
                c.getString(1), c.getString(2), c.getString(3));

        dbase.close();
        return retObj;
    }

    public ArrayList<String>[] getActiveNumbers() {
        ArrayList<String>[] activeInfo = new ArrayList[2];
        ArrayList<String> nums = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 1; i < this.getRecordCount(); i++) {
            //get phone number out of message object and add it to array list
            String pNum = this.getSingleMessage(i).get_number();

            String cName = this.getSingleMessage(i).get_name();
            if (!nums.contains(pNum)) {
                nums.add(pNum);
                names.add(cName);
            } else {

            }

            //if(!names.contains(cName)) names.add(cName);
        }
        activeInfo[0] = nums;
        activeInfo[1] = names;

        return activeInfo;
    }

    public ArrayList<String> getConversation(String no) {
        ArrayList<String> ret = new ArrayList<String>();
        for (int i = 1; i < this.getRecordCount(); i++) {
            if (this.getSingleMessage(i).get_number().equalsIgnoreCase(no)) {
                ret.add(this.getSingleMessage(i).getBody());
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
                new String[]{String.valueOf(msg.get_id())});
        dbase.close();
    }
}
