package com.bccs.bsecure;

import java.util.Calendar;

/**
 * Created by traci.kamp on 11/11/2014.
 * Created as a more efficient way of storing outgoing
 * messages in an app-created SQLite database since
 * the app cannot currently write to Android's default
 * outbox provider (default SMS app functionality not
 * implemented due to time constraints).
 *
 * Modified by lucas.burdell 6/5/2015.
 * Reorganized class now prepares a time stamp and
 * a boolean check for whether the message was sent or received.
 * This class may be temporary for when we prepare a more
 * robust database to retrieve contact information.
 *
 * Modified by lucas.burdell 6/12/2015.
 * Added boolean to check whether a message is encrypted or not.
 */

public class myMessage {
    private int _id;
    private String _name = "dummy"; // TODO: Add name functionality to database
    private String _number;
    private String _body;
    private boolean _sent = true;
    private int _time;
    private boolean isDHKey = false;
    private boolean _encrypted = false;

    // Empty constructor
    public myMessage() {
    }

    // non-specific constructor
    public myMessage(String number, String body) {
        this(number, body, true);
    }

    public myMessage(String number, String body, boolean sent) {
        this._name = number;
        this._number = number;
        this._body = body;
        this._sent = sent;
        this._time = Calendar.getInstance().get(Calendar.SECOND); // get current time in seconds
    }

    // Getter and setter methods, generated by Android Studio
    public String get_name() {
        return _name;
    }

    public String get_number() {
        return _number;
    }

    public String getBody() {
        return _body;
    }

    // General toString method. Prints contact name, number, and message body.
    public String toString() {
        return "Name: " + get_name() + " \nNumber: " + get_number() + " \nMessage: " + getBody();
    }

    public boolean getSent() {
        return this._sent;
    }

    public void setId(int id) {
        this._id = id;
    }

    public int getId() {
        return this._id;
    }

    public int get_time() {
        return _time;
    }

    public void set_time(int _time) {
        this._time = _time;
    }

    public boolean isDHKey() {
        return isDHKey;
    }

    public void setIsDHKey(boolean isDHKey) {
        this.isDHKey = isDHKey;
    }

    public boolean is_encrypted() {
        return _encrypted;
    }

    public void set_encrypted(boolean _encrypted) {
        this._encrypted = _encrypted;
    }
}
