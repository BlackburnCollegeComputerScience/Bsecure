package com.bccs.bsecure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
/*
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

/*
    created by lucas.burdell 6/18/2015
 */
public class ConversationManager extends SQLiteOpenHelper {


    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SPACE_SEP = " ";

    //Table to store messages of a conversation
    private static final String TABLE_MESSAGES_TEMPLATE = "messages";

    //Table Info
    private static final String COLUMN_ID = "id"; //ID of message
    private static final String COLUMN_BODY = "msg_body"; // body of the message
    private static final String COLUMN_SENT = "sent"; // whether it was sent or received
    private static final String COLUMN_TIME = "msg_time"; // time message sent / received
    private static final String COLUMN_ENC = "msg_encrypted";
    //Database Info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ConversationDB.db";


    //Table to store conversation db data
    private static final String TABLE_CONVERSATIONS = "conversations";
    //Table info
    private static final String COLUMN_CONTACT_ID = "conv_contact_id"; // The conversation contact's number

    private static ConversationManager sInstance = null;
    private static SQLiteDatabase sDatabase = null;

    public ConversationManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        sDatabase = this.getWritableDatabase();
        onCreate(sDatabase);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMasterTable = "CREATE TABLE IF NOT EXISTS " + TABLE_CONVERSATIONS + "(" + COLUMN_ID +
                INT_TYPE + " PRIMARY KEY AUTOINCREMENT" + COMMA_SEP + COLUMN_CONTACT_ID + INT_TYPE + ")";
        db.execSQL(createMasterTable);

    }

    public static ConversationManager getManager(Context context) {
        if (sInstance == null) {
            sInstance = new ConversationManager(context.getApplicationContext());
        }
        return sInstance;
    }

    public static ConversationHelper getConversation(Context context, long contactid) {
        if (sInstance==null) {
            sInstance = new ConversationManager(context.getApplicationContext());
        }
        ConversationHelper conversationHelper = new ConversationHelper(sInstance, contactid);
        sInstance.addMasterRecord(contactid);
        return conversationHelper;
    }

    public static ConversationHelper getConversation(long contactid) {
        sInstance.addMasterRecord(contactid);
        return new ConversationHelper(sInstance, contactid);
    }

    public void clearAllConversations() {
        String countQuery = "SELECT * FROM " + TABLE_CONVERSATIONS;
        Cursor cursor = sDatabase.rawQuery(countQuery, null);
        if (cursor!=null) {
            try {
                while (!cursor.isLast()) {
                    cursor.moveToNext();
                    removeMasterRecord(cursor.getInt(1));
                }
                cursor.close();
            } catch (Exception ignored) {
                cursor.close();
            }
        }
        sDatabase.delete(TABLE_CONVERSATIONS, null, null);
    }

    private int getMasterRecordID(long contactid) {
        String select = "SELECT * FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_CONTACT_ID + "=?";
        Cursor c = sDatabase.rawQuery(select, new String[]{"" + contactid});
        int ret = -1;
        if (c != null) {
            try {
                c.moveToFirst();
                ret = c.getInt(0);
            } catch (Exception ignored) { }
            c.close();
        }
        return ret;
    }
    private void addMasterRecord(long contactid) {
        if (getMasterRecordID(contactid)==-1) {
            try {
                ContentValues vals = new ContentValues();
                vals.put(COLUMN_CONTACT_ID, contactid);
                sDatabase.insert(TABLE_CONVERSATIONS, null, vals);

            } catch (Exception ignored) {
                ignored.printStackTrace();
                sDatabase.delete(TABLE_CONVERSATIONS, COLUMN_ID + "=" +
                        getRecordCount(TABLE_CONVERSATIONS) + 1, null);
            }
            
        } else {

        }
    }

    private void removeMasterRecord(long contactid) {
        try {
            sDatabase.delete(TABLE_MESSAGES_TEMPLATE + contactid, null, null);
            sDatabase.delete(TABLE_CONVERSATIONS, COLUMN_CONTACT_ID + " = " + contactid, null);
        } catch (Exception ignored) {ignored.printStackTrace();}
    }

    static class ConversationHelper {
        private final String tableName;
        private final long contactid;
        private final ConversationManager db;
        public ConversationHelper(ConversationManager db, long contactid) {
            this.db = db;
            this.tableName = TABLE_MESSAGES_TEMPLATE + contactid;
            this.contactid = contactid;
            String createConversation = "CREATE TABLE IF NOT EXISTS " + tableName + "(" +
                    COLUMN_ID + SPACE_SEP + INT_TYPE + " PRIMARY KEY" +
                    COMMA_SEP + COLUMN_BODY + SPACE_SEP + TEXT_TYPE + COMMA_SEP + COLUMN_SENT +
                    SPACE_SEP + INT_TYPE + COMMA_SEP + COLUMN_TIME + SPACE_SEP + TEXT_TYPE +
                    COMMA_SEP + COLUMN_ENC + SPACE_SEP + INT_TYPE + ")";
            sDatabase.execSQL(createConversation);
        }

        public ArrayList<MyMessage> getMessages() {
            ArrayList<MyMessage> ret = new ArrayList<>();
            for (int i = 1; i <= db.getRecordCount(this.tableName); i++) {
                MyMessage singleMessage = this.getSingleMessage(i);
                ret.add(singleMessage);
            }
            return ret;
        }

        public MyMessage getLastMessage() {
            return this.getSingleMessage(this.db.getRecordCount(this.tableName));
        }

        public ArrayList<MyMessage> getMessages(int lastMessageId) {
            ArrayList<MyMessage> ret = new ArrayList<>();
            int recordCount = db.getRecordCount(this.tableName);
            if (recordCount == 0) return ret;
            if (lastMessageId == recordCount) return ret;
            for (int i = lastMessageId + 1; i <= db.getRecordCount(this.tableName);i++) {
                ret.add(this.getSingleMessage(i));
            }
            return ret;
        }

        public void addMessage(MyMessage msg) {
            try {
                ContentValues vals = new ContentValues();

                vals.put(COLUMN_ID, this.db.getRecordCount(this.tableName) + 1);
                vals.put(COLUMN_BODY, msg.getBody());
                vals.put(COLUMN_SENT, msg.getSent() ? 1 : 0);
                vals.put(COLUMN_TIME, ((Long) msg.get_time()).toString());
                vals.put(COLUMN_ENC, msg.is_encrypted() ? 1 : 0);

                sDatabase.insert(this.tableName, null, vals);

            } catch (Exception ignored) {ignored.printStackTrace();}
        }

        private MyMessage getSingleMessage(int id) {
            String select = "SELECT * FROM " + this.tableName + " WHERE " + COLUMN_ID + "=?";
            Cursor c = sDatabase.rawQuery(select, new String[]{id + ""});
            if (c != null && c.getCount()>0) {
                MyMessage retObj = null;
                try {
                    c.moveToFirst();
                    String body = c.getString(1);
                    boolean sent = c.getInt(2) == 1;
                    boolean encrypted = c.getInt(4) == 1;
                    long time = Long.parseLong(c.getString(3));

                    retObj = new MyMessage(this.contactid, body, sent);
                    retObj.set_time(time);
                    retObj.set_encrypted(encrypted);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                c.close();
                return retObj;
            }
            return null;
        }

        public void deleteConversation() {
            sDatabase.delete(this.tableName, null, null);
            this.db.removeMasterRecord(this.contactid);
        }

    }

    //gets a count of records stored in DB
    private int getRecordCount(String tableName) {
        String countQuery = "SELECT * FROM " + tableName;
        int count = 0;
        try {
            Cursor cursor = sDatabase.rawQuery(countQuery, null);
            count = cursor.getCount();
            cursor.close();
        } catch(Exception ignored) { }
        
        return count;
    }

    public static void closeConnection() {
        if (sInstance !=null && sDatabase != null) {
            sInstance.close();
            sDatabase.close();
            sInstance = null;
            sDatabase = null;
        }
    }

    public ArrayList<Integer> getActiveConversations() {
        ArrayList<Integer> output = new ArrayList<>();
        String countQuery = "SELECT * FROM " + TABLE_CONVERSATIONS;
        Cursor cursor = sDatabase.rawQuery(countQuery, null);
        if (cursor!=null && cursor.getCount()>0) {
            try {
                while (!cursor.isLast()) {
                    cursor.moveToNext();
                    output.add(cursor.getInt(1));
                }
                cursor.close();
            } catch (Exception ignored) {
                ignored.printStackTrace();
                cursor.close();
            }
        }
        
        return output;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO write onUpgrade
    }
}