package com.bccs.bsecure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/*
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
        public static final String COLUMN_NAME_CURRENT_SEQ = "currentseq";
        public static final String COLUMN_NAME_MAX_SEQ = "maxseq";
        public static final String COLUMN_NAME_TOTAL_KEYS = "totalkeys";
        public static final String COLUMN_NAME_EXPIRE_TIME = "expire";
        public static final String COLUMN_NAME_USES_LEFT = "uses";
        public static final String COLUMN_NAME_USES_MAX = "maxuses";


        public static final String SQL_CREATE_SC_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_CONTACT_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_CURRENT_SEQ + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_MAX_SEQ + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_TOTAL_KEYS + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_EXPIRE_TIME + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_USES_LEFT + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_USES_MAX + INT_TYPE +
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
        public static final String COLUMN_NAME_IV = "iv";
        public static final String SQL_CREATE_KEY_PAIR_ENTRIES = "CREATE TABLE " + TABLE_NAME +
                " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_CONTACT_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_SEQ_NUM + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_KEY + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_IV + TEXT_TYPE +
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

    public int getCollumnFromIdAsInt(String tableName, String collumn, long id) {
        System.out.println("Looking in " + tableName + "for " + collumn + " with id " + id);
        String select = "SELECT * FROM " + tableName + " WHERE " + SCEntry.COLUMN_NAME_CONTACT_ID + " = " + id;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {

                c.moveToFirst();
                return c.getInt(c.getColumnIndex(collumn));
            } else {
                System.out.println("CURSOR FOR " + collumn + " with id " + id + " was empty!");
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void setCollumnFromId(String tableName, String collumn, int value, long id) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(collumn, value);
        dbase.update(tableName, vals, SCEntry.COLUMN_NAME_CONTACT_ID + "=?", new String[]{Long.toString(id)});
    }

    public void setCollumnFromId(String tableName, String collumn, String value, long id) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(collumn, value);
        dbase.update(tableName, vals, SCEntry.COLUMN_NAME_CONTACT_ID + "=?", new String[]{Long.toString(id)});
    }

    public String getCollumnFromId(String tableName, String collumn, long id) {
        String select = "SELECT * FROM " + tableName + " WHERE ?=?";
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, new String[]{collumn, id + ""});
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getString(c.getColumnIndex(collumn));
            } else {
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //original version, no upgrades to handle yet
    }


    public void createSecurityContact(SecurityContact sc) {
        //get writable db to put new contact in
        SQLiteDatabase db = this.getWritableDatabase();

        //make values to insert
        ContentValues values = new ContentValues();

        //if security contact already exists, don't create duplicate
        //else create from sc info
        values.put(SCEntry.COLUMN_NAME_CONTACT_ID, sc.getId());
        values.put(SCEntry.COLUMN_NAME_CURRENT_SEQ, sc.getSeqNum());
        values.put(SCEntry.COLUMN_NAME_MAX_SEQ, sc.getSeqMax());
        values.put(SCEntry.COLUMN_NAME_TOTAL_KEYS, sc.getTotalKeys());
        values.put(SCEntry.COLUMN_NAME_EXPIRE_TIME, sc.getTimeLeft());
        values.put(SCEntry.COLUMN_NAME_USES_LEFT, sc.getUsesLeft());
        values.put(SCEntry.COLUMN_NAME_USES_MAX, sc.getUsesMax());

        if (getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_CONTACT_ID, sc.getId())!=-1) {
            db.update(SCEntry.TABLE_NAME, values,
                    SCEntry.COLUMN_NAME_CONTACT_ID + "=" + sc.getId(), null);
        } else {
            System.out.println("Inserting contact " + sc.getId());
            System.out.println("Total keys: " + sc.getTotalKeys());
            System.out.println("Contact id: " + values.get(SCEntry.COLUMN_NAME_CONTACT_ID));
            System.out.println("TOTAL KEYS: " + values.get(SCEntry.COLUMN_NAME_TOTAL_KEYS));
            db.insert(SCEntry.TABLE_NAME, null, values);
        }
        db.close();
    }

    public boolean contactIsInDatabase(long id) {
        boolean b = getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_CONTACT_ID, id) != -1;
        System.out.println("id " + id + " was in database: " + b);
        return b;
    }



    public int getContactSeqNum(long id) {
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
            }
            return new int[]{0, 0};
        } catch (Exception e) {
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



    public int getContactSeqMax(long id) {
        return getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_MAX_SEQ, id);
    }

    public int getContactTotalKeys(long id) {
        return getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_TOTAL_KEYS, id);
    }

    public int getContactTimeLeft(long id) {
        return getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_EXPIRE_TIME, id);
    }

    public int getContactUsesLeft(long id) {
        return getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_USES_LEFT, id);
    }

    public int getContactUsesMax(long id) {
        return getCollumnFromIdAsInt(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_USES_MAX, id);
    }

    public void setContactSeqMax(long id, int value) {
        setCollumnFromId(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_MAX_SEQ, value, id);
    }

    public void setContactTotalKeys(long id, int value) {
        setCollumnFromId(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_TOTAL_KEYS, value, id);
    }

    public void setContactTimeLeft(long id, int value) {
        setCollumnFromId(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_EXPIRE_TIME, value, id);
    }

    public void setContactUsesLeft(long id, int value) {
        setCollumnFromId(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_USES_LEFT, value, id);
    }

    public void setContactUsesMax(long id, int value) {
        setCollumnFromId(SCEntry.TABLE_NAME, SCEntry.COLUMN_NAME_USES_MAX, value, id);
    }


    public int getNextSequence(long id) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newSeq;
    }


    public void clearKey(int seq, long id) {
        SQLiteDatabase dbase = this.getWritableDatabase();
        if (keyExists(seq, id)) {
            ContentValues vals = new ContentValues();
            vals.put(KeyPairEntry.COLUMN_NAME_CONTACT_ID, id);
            vals.put(KeyPairEntry.COLUMN_NAME_KEY, "");
            vals.put(KeyPairEntry.COLUMN_NAME_SEQ_NUM, seq);
            dbase.update(KeyPairEntry.TABLE_NAME, vals,
                    KeyPairEntry.COLUMN_NAME_CONTACT_ID + "=? AND " +
                    KeyPairEntry.COLUMN_NAME_SEQ_NUM + "=?",
                    new String[]{Long.toString(id), Integer.toString(seq)});
        }
    }

    public boolean keyExists(int seq, long id) {
        String select = "SELECT * FROM " + KeyPairEntry.TABLE_NAME + " WHERE " +
                KeyPairEntry.COLUMN_NAME_CONTACT_ID + "=" + id + " AND " +
                KeyPairEntry.COLUMN_NAME_SEQ_NUM + "=" + seq;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String key = c.getString(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_KEY));
                c.close();
                return !(key.equals(""));
            }
            return false;
        } catch (Exception ignored) { }
        return false;
    }


    public int addKeys(String[] keys, String[] IVs, long id, int seq, int maxSeq, int totalKeys) {
        SQLiteDatabase database = this.getWritableDatabase();
        database.beginTransaction();
        int newMaxSeq = Math.abs((maxSeq + 1) % totalKeys);;
        for (int i = 0; i < keys.length; i++) {
            if (seq == newMaxSeq) break;
            ContentValues vals = new ContentValues();
            vals.put(KeyPairEntry.COLUMN_NAME_KEY, keys[i]);
            vals.put(KeyPairEntry.COLUMN_NAME_IV, IVs[i]);
            vals.put(KeyPairEntry.COLUMN_NAME_CONTACT_ID, id);
            vals.put(KeyPairEntry.COLUMN_NAME_SEQ_NUM, newMaxSeq);
            if (keyExists(newMaxSeq, id)) {
                database.update(KeyPairEntry.TABLE_NAME, vals,
                        KeyPairEntry.COLUMN_NAME_CONTACT_ID + "=? AND " +
                                KeyPairEntry.COLUMN_NAME_SEQ_NUM + "=?",
                        new String[]{Long.toString(id), Integer.toString(newMaxSeq)});
            } else {
                database.insert(KeyPairEntry.TABLE_NAME, null, vals);
            }
            newMaxSeq = Math.abs((newMaxSeq + 1) % totalKeys);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return newMaxSeq;
    }

    public int addKeys(String[] keys, long id, int seq, int maxSeq, int totalKeys) {
        SQLiteDatabase database = this.getWritableDatabase();
        database.beginTransaction();
        int newMaxSeq = Math.abs((maxSeq + 1) % totalKeys);;
        for (String key : keys) {
            if (seq == newMaxSeq) break;
            ContentValues vals = new ContentValues();
            vals.put(KeyPairEntry.COLUMN_NAME_KEY, key);
            vals.put(KeyPairEntry.COLUMN_NAME_CONTACT_ID, id);
            vals.put(KeyPairEntry.COLUMN_NAME_SEQ_NUM, newMaxSeq);
            if (keyExists(newMaxSeq, id)) {
                database.update(KeyPairEntry.TABLE_NAME, vals,
                        KeyPairEntry.COLUMN_NAME_CONTACT_ID + "=? AND " +
                                KeyPairEntry.COLUMN_NAME_SEQ_NUM + "=?",
                        new String[]{Long.toString(id), Integer.toString(newMaxSeq)});
            } else {
                database.insert(KeyPairEntry.TABLE_NAME, null, vals);
            }
            newMaxSeq = Math.abs((newMaxSeq + 1) % totalKeys);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return newMaxSeq;
    }

    public void clearKeys(long id) {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(KeyPairEntry.TABLE_NAME, "id=?", new String[]{Long.toString(id)});
    }

    public void clearKeys() {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(KeyPairEntry.TABLE_NAME, null, null);
    }


    public String[] getKeyPair(long id, int seqNum) {
        String select = "SELECT * FROM " + KeyPairEntry.TABLE_NAME + " WHERE " +
                KeyPairEntry.COLUMN_NAME_CONTACT_ID + " = " + id + " AND " +
                KeyPairEntry.COLUMN_NAME_SEQ_NUM + " = " + seqNum;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String[] key = new String[]{
                        c.getString(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_KEY)),
                        c.getString(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_IV))
                };

                c.close();
                return key;
            } else {

            }
            return new String[]{null, null};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[]{null, null};
    }

    public String getKey(long id, int seqNum) {
        String select = "SELECT * FROM " + KeyPairEntry.TABLE_NAME + " WHERE " +
                KeyPairEntry.COLUMN_NAME_CONTACT_ID + " = " + id + " AND " +
                KeyPairEntry.COLUMN_NAME_SEQ_NUM + " = " + seqNum;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String key = c.getString(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_KEY));
                c.close();
                return key;
            } else {

            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getIV(long id, int seqNum) {
        String select = "SELECT * FROM " + KeyPairEntry.TABLE_NAME + " WHERE " +
                KeyPairEntry.COLUMN_NAME_CONTACT_ID + " = " + id + " AND " +
                KeyPairEntry.COLUMN_NAME_SEQ_NUM + " = " + seqNum;
        SQLiteDatabase dbase = this.getReadableDatabase();
        try {
            Cursor c = dbase.rawQuery(select, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String key = c.getString(c.getColumnIndex(KeyPairEntry.COLUMN_NAME_IV));
                c.close();
                return key;
            } else {

            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
