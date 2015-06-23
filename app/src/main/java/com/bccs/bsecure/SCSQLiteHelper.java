package com.bccs.bsecure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 *
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 *
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 * Creates a two table database that stores all relevant security contact settings and
 * values for key generation.
 *      "scentry" table stores basic settings, and links to main contacts db by _id
 *      "keypairentry" stores all pairs of values per contact, also links by main contacts _id
 * Created by kevin.coogan on 6/15/2015.
 */
public class SCSQLiteHelper extends SQLiteOpenHelper {
    //must increment database_VERSION when db schema change
    private static final int database_VERSION = 1;

    //database info
    private static final String database_NAME = "BSecureContactsDB";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    public static abstract class GeneralSettings implements BaseColumns {
        public static final String TABLE_NAME = "generalSetting";
        public static final String COLUMN_NAME_MINIMUM_EXPIRE_COUNT = "min";
        public static final String COLUMN_NAME_MAXIMUM_EXPIRE_COUNT = "max";

        public static final String SQL_CREATE_GENERAL_SETTINGS = "CREATE TABLE " + TABLE_NAME
                + " (" + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME_MINIMUM_EXPIRE_COUNT + INT_TYPE
                + COMMA_SEP + COLUMN_NAME_MAXIMUM_EXPIRE_COUNT + INT_TYPE + ") ";

    }

    /*
        "scentry" table stores basic security settings for contact. Should be one entry per
                secure contact. Contacts from main android db that are not initialized into
                secure app will not have an entry.

         "contactid" -- _id from main android contacts. Links our data to android data.
         "currentseq" -- separate table stores list of value pairs used to generate next session
                        key. each pair is numbered sequentially, and currentseq indicates which
                        pair is currently being used.
         "degrees" -- security setting that indicates how many degrees of separation are trusted.

     */
    public static abstract class SCEntry implements BaseColumns {
        public static final String TABLE_NAME = "scentry";
        public static final String COLUMN_NAME_CONTACT_ID = "contactid";
        public static final String COLUMN_NAME_NAME ="contactname";
        public static final String COLUMN_NAME_CURRENT_SEQ = "currentseq";
        public static final String COLUMN_NAME_EXPIRE_TIME = "expire";
        public static final String COLUMN_NAME_USES_LEFT = "uses";


        public static final String SQL_CREATE_SC_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_CONTACT_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_CURRENT_SEQ + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_EXPIRE_TIME + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_USES_LEFT + INT_TYPE +
                " )";

        //public static final String SQL_DELETE_SC_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

    }

    /*
        "keypairentries" -- separate table whose purpose is to store key pairs used to generate
            session keys and link them back to main scentry table
         "contactid" -- same as "contactid" in "scentry" table, and same as _id of main contact
                in android contacts db
         "seqnum" -- each key pair has sequence number. key pairs are used to generate session key,
                and next seq number to use if last seqnum + 1. Should allow overflow to 0 and still
                work properly.
         "avalue" -- current implementation uses Diffie-Hellman pairs. avalue represents a in the
                equation g^(ab) mod p. self generated before exchange.
         "bvalue" -- represents g^b mod in the equation g^(ab) mod. received from exchange.

          note: avalue and bvalue are currently listed as text_type. Likely to be large hex
                values, and storing as string and converting to values is probably better
                than trying to store 256 or 512-bit data type.

     */
    public static abstract class KeyPairEntry implements BaseColumns {
        public static final String TABLE_NAME = "keypairentry";
        public static final String COLUMN_NAME_CONTACT_ID = "contactid";
        public static final String COLUMN_NAME_SEQ_NUM = "seqnum";
        public static final String COLUMN_NAME_KEY = "key";

        //TODO: DO I NEED TO AUTOINCREMENT PRIMARY KEY???
        public static final String SQL_CREATE_KEY_PAIR_ENTRIES = "CREATE TABLE " + TABLE_NAME +
                " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_CONTACT_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_SEQ_NUM + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_KEY + TEXT_TYPE +
                " )";

        //public static final String SQL_DELETE_KEY_PAIR_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

    }
    //default constructor
    public SCSQLiteHelper(Context context) {
        super(context, database_NAME, null, database_VERSION);
    }

    public void clearDatabase() {
        SQLiteDatabase dbase = this.getReadableDatabase();
        dbase.delete(SCEntry.TABLE_NAME, null, null);
        dbase.delete(KeyPairEntry.TABLE_NAME, null, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create tables
        db.execSQL(SCEntry.SQL_CREATE_SC_ENTRIES);
        db.execSQL(KeyPairEntry.SQL_CREATE_KEY_PAIR_ENTRIES);
        db.execSQL(GeneralSettings.SQL_CREATE_GENERAL_SETTINGS);

        ContentValues vals = new ContentValues();
        vals.put(GeneralSettings.COLUMN_NAME_MINIMUM_EXPIRE_COUNT, 25);
        vals.put(GeneralSettings.COLUMN_NAME_MAXIMUM_EXPIRE_COUNT, 100);
        db.insert(GeneralSettings.TABLE_NAME, null, vals);
    }

    public int getCollumnFromIdAsInt(String tableName, String collumn, int id) {
        String select = "SELECT * FROM " + tableName + " WHERE " + collumn + " = " + id;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getInt(c.getColumnIndex(collumn));
            } else {
                System.out.println("Cursor was empty or null - LB");
            }
            return 0;
        } catch (Exception e) {
            System.out.println("Table did not exist or was empty - LB");
            e.printStackTrace();
        }
        return 0;
    }


    public String getCollumnFromId(String tableName, String collumn, int id) {
        String select = "SELECT * FROM " + tableName + " WHERE " + collumn + " = " + id;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getString(c.getColumnIndex(collumn));
            } else {
                System.out.println("Cursor was empty or null - LB");
            }
            return "Empty";
        } catch (Exception e) {
            System.out.println("Table did not exist or was empty - LB");
            e.printStackTrace();
        }
        return "Empty";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //original version, no upgrades to handle yet
    }

    public void createSecurityContact(SecurityContact sc) throws Exception {
        //get writable db to put new contact in
        SQLiteDatabase db = this.getWritableDatabase();

        //make values to insert
        ContentValues values = new ContentValues();

        //if security contact already exists, don't create duplicate
        //else create from sc info
        values.put(SCEntry.COLUMN_NAME_CONTACT_ID, sc.getID());
        values.put(SCEntry.COLUMN_NAME_NAME, sc.getName());
        values.put(SCEntry.COLUMN_NAME_CURRENT_SEQ, sc.getSeqNum());
        //TODO: Need other settings as they occur

        long retVal = db.insert(SCEntry.TABLE_NAME, null, values);
        if(retVal < 0) { // error
            //throw exception??
            throw new Exception("SQL exception - LB");
        }
        db.close();
    }

    public String getContactName(int id) {
        return getCollumnFromId(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_NAME, id);
    }

    public int getContactIdFromName(String name) {
        String select = "SELECT * FROM " + SCEntry.TABLE_NAME + " WHERE " + SCEntry.COLUMN_NAME_NAME
                + " = " + name;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getInt(c.getColumnIndex(SCEntry.COLUMN_NAME_CONTACT_ID));
            } else {
                System.out.println("Cursor was empty or null - LB");
            }
            return 0;
        } catch (Exception e) {
            System.out.println("Table did not exist or was empty");
            e.printStackTrace();
        }
        return 0;
    }


    public int getContactSeqNum(int id) {
        return getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_CURRENT_SEQ, id);
    }

    public int[] getGeneralSettings() {
        String select = "SELECT * FROM " + GeneralSettings.TABLE_NAME;
        SQLiteDatabase dbase = this.getReadableDatabase();
        int min = 0;
        int max = 0;
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                min = c.getInt(c.getColumnIndex(GeneralSettings.COLUMN_NAME_MINIMUM_EXPIRE_COUNT));
                max = c.getInt(c.getColumnIndex(GeneralSettings.COLUMN_NAME_MAXIMUM_EXPIRE_COUNT));
                return new int[]{min, max};
            } else {
                System.out.println("Cursor was empty or null - LB");
            }
            return new int[]{0, 0};
        } catch (Exception e) {
            System.out.println("Table did not exist or was empty - LB");
            e.printStackTrace();
        }
        return new int[]{0, 0};
    }

    public void setGeneralSettings(int min, int max) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(GeneralSettings.COLUMN_NAME_MINIMUM_EXPIRE_COUNT, min);
        vals.put(GeneralSettings.COLUMN_NAME_MAXIMUM_EXPIRE_COUNT, max);
        dbase.update(GeneralSettings.TABLE_NAME, vals, null, null);
    }

    public int getNextSequence(int id) {
        String select = "SELECT * FROM " + SCEntry.TABLE_NAME + " WHERE " + SCEntry.COLUMN_NAME_CONTACT_ID
                + " = " + id;
        SQLiteDatabase dbase = this.getReadableDatabase();
        int newSeq = 0;
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    int seq = c.getInt(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_SEQ_NUM));
                    if (seq>newSeq) newSeq = seq;
                }
            } else {
                System.out.println("Cursor was empty or null - LB");
            }
        } catch (Exception e) {
            System.out.println("Table did not exist or was empty - LB");
            e.printStackTrace();
        }
        return newSeq;
    }

    public String getNextKey(int id, int seqNum) {
        String select = "SELECT * FROM " + KeyPairEntry.TABLE_NAME + " WHERE " +
                KeyPairEntry.COLUMN_NAME_CONTACT_ID + " = " + id + " AND " +
                KeyPairEntry.COLUMN_NAME_SEQ_NUM + " = " + seqNum;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getString(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_KEY));
            } else {
                System.out.println("Cursor was empty or null - LB");
            }
            return null;
        } catch (Exception e) {
            System.out.println("Table did not exist or was empty");
            e.printStackTrace();
        }
        return null;
    }
}
