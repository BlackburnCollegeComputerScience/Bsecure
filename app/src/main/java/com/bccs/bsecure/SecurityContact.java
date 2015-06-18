package com.bccs.bsecure;

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

public class SecurityContact {
    private int id;         //this should be primary _ID for contact from android db
    private String name;
    private int seqNum;
    private String currKey;
    private int degrees;

    //these values won't be ints. these are just dummy's to get database setup and understood
    private int aValue;     //self generated, a in g^(ab) mod p
    private int bValue;     //received from b: g^b mod p in g^(ab) mod p

    /* need empty constructor */
    public SecurityContact() {}

    public SecurityContact(int id) {
        this.id = id;

        //Lookup in android db with id, if not there throw exception
        this.name = "Name";

        //Lookup in SC db.scentry table using id
        //if exists, BSecure contact exists, use db values
        //else, BSecure contact does not exist, create with default values
        this.seqNum = 0;
        this.degrees = 2;           //Lookup in SC db.scentry table using id

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

    public int getDegrees() {
        return degrees;
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
}
