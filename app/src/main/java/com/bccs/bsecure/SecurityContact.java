package com.bccs.bsecure;

/**
 * Created by kevin.coogan on 6/15/2015.
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
