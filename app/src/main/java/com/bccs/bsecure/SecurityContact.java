package com.bccs.bsecure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
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

public class SecurityContact implements Serializable {
    private int id;         //this should be primary _ID for contact from android db
    private String name;
    private String number;
    private int seqNum;
    private String currKey;



    /* need empty constructor */

    public SecurityContact() {
    }

    public SecurityContact(int id, String name, String number) {
        this.name = name;
        this.number = number;
        this.id = id;
        this.seqNum = 0;


    }

    public SecurityContact(int id) {
        this.id = id;

        //Lookup in android db with id, if not there throw exception
        this.name = "Name";

        //Lookup in SC db.scentry table using id
        //if exists, BSecure contact exists, use db values
        //else, BSecure contact does not exist, create with default values
        this.seqNum = 0;

        //Lookup in SC db.keypairentry table using id and seqnum
        //if does not exist, no more key pairs available. Throw exception??
        this.currKey = "0123ABC";
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSessionKey() {
        if (keyExpired()) {
            currKey = getNextKey();      //lookup in SC db.keypairentry using id and seqnum
        }

        return currKey;
    }

    public int getSeqNum() {
        return seqNum;
    }

    private boolean keyExpired() {
        //TODO: Need expiration setting to implement
        return false;
    }

    private String getNextKey() {
        //TODO: lookup key at location seqnum + 1
        // must handle overflow to 0!
        // if no more keys, prompt user to use old key or abort (maybe throw exception here)
        return "Next";
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(this);
        return b.toByteArray();
    }

    /**
     * Method for de-serializing the SecurityContact (for passing the contact between activities
     *
     public static SecurityContact deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
     ByteArrayInputStream b = new ByteArrayInputStream(bytes);
     ObjectInputStream o = new ObjectInputStream(b);
     return (SecurityContact) o.readObject();
     }
     */
}
