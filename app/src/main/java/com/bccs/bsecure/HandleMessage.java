package com.bccs.bsecure;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

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
 * Created by lucas.burdell on 6/4/2015.
 * This file will handle sending outbound messages, which includes setting the header and calling
 * the cipher module for encryption.
 * <p>
 * Modified by shane.nalezyty on 6/9/2015
 * Edited the Sms receiving method to recognise sms split into parts and append them back together
 * before decryption.
 * <p>
 * Modified by lucas.burdell on 6/12/2015
 * Changed original temporary keys to a Diffie-Hellman generated key and an SHA-256 hash of the
 * that key. Both were converted to hexadecimal represented in a string.
 * Added very basic functionality to database to store and retrieve DH keys
 */
public class HandleMessage {

    //These are temp keys
    //private static final String key1 = "524F0D82AFA4779CB7A55358798117A88A90549730659C0778CEBE5BAD7FDD77";
    private static final String key2 = "NVzTTl4v9Z0jOL2qyNuOFodEhN8ArwZIG4RJ9g8zQlI=";


    private static final String prepend = "-&*&-"; // current message header
    private static final String expireKey = "*-**-*";
    private static final String expireAllKeys = "&-&&-&";
    private static final String prependDH = "*-&-*"; // header for DH key exchanges



    public static void sendKeyExpired(SecurityContact contact) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(contact.getNumber(), null, expireKey, null, null);
    }

    public static void sendAllkeysExpired(SecurityContact contact) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(contact.getNumber(), null, expireAllKeys, null, null);
    }

    /**
     * send method for sending messages to a number
     *
     * @param contactid the contactid to send to
     * @param msg       message to send
     * @return myMessage object to be used by UI
     */

    public static MyMessage send(long contactid, String msg, Context context) {
        return send(contactid, msg, context, false);
    }


    public static MyMessage send(long contactid, String msg, Context context, boolean isDH) {
        Contact contact = new Contact(context, contactid);
        if (!isDH) {

            SmsManager sms = SmsManager.getDefault();

            MyMessage msgObj = new MyMessage(contactid, msg, true);


            //PULL FROM DB
            String key = null;
            SecurityContact sContact = null;
            if (SecurityContact.contactIdIsASecurityContact(contactid)) {
                sContact = new SecurityContact(contactid);
                key = sContact.getSessionKey();
            }

            if (key != null) {
                msg = MessageCipher.encrypt(msg, key, key2);
                msg = getPrepend() + msg;
            }

            //new multipart text messages
            ArrayList<String> messages = sms.divideMessage(msg);
            int numberOfParts = messages.size();


            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveryIntents = new ArrayList<>();

            Intent mSendIntent = new Intent("CTS_SMS_SEND_ACTION");
            Intent mDeliveryIntent = new Intent("CTS_SMS_DELIVERY_ACTION");

            for (int i = 0; i < numberOfParts; i++) {
                sentIntents.add(PendingIntent.getBroadcast(context, 0, mSendIntent, 0));
                deliveryIntents.add(PendingIntent.getBroadcast(context, 0, mDeliveryIntent, 0));
            }


            sms.sendMultipartTextMessage(contact.getNumber(), null, messages, sentIntents, deliveryIntents);
            msgObj.set_encrypted(key != null);


            return msgObj;
        } else {

            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> messages = sms.divideMessage(msg);
            int numberOfParts = messages.size();
            System.out.printf("Total: " + msg.length());
            MyMessage msgObj = new MyMessage(contactid, msg, true);
            msgObj.setIsDHKey(true);
            sms.sendMultipartTextMessage(contact.getNumber(), null, messages, null, null);

            return msgObj;
        }
    }


    /**
     * Handle outgoing messages not sent by this application
     *
     * @param intent Intent from received message
     * @return myMessage object to represent message
     */
    public static MyMessage handleOutgoingMessage(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey("pdus")) {
            //Get Sms objects
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus.length == 0) {
                return null;
            }
            //Large messages might be broken into an array
            SmsMessage[] smsMessages = new SmsMessage[pdus.length];
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pdus.length; i++) {
                smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                stringBuilder.append(smsMessages[i].getMessageBody());
            }
            String sender = smsMessages[0].getOriginatingAddress();
            String message = stringBuilder.toString();

            MyMessage msgObj = new MyMessage(Contact.getIdFromNumber(sender), message);
            msgObj.set_encrypted(false);
            return msgObj;
        }
        return null;
    }

    /**
     * Handle incoming message and return myMessage object to be added to the DB
     *
     * @param intent Intent from the received message
     * @return myMessage object to represent the message
     */
    public static MyMessage handleIncomingMessage(Intent intent, Context context) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey("pdus")) {
            //Get Sms objects
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus.length == 0) {
                return null;
            }
            //Large messages might be broken into an array
            SmsMessage[] smsMessages = new SmsMessage[pdus.length];
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pdus.length; i++) {
                smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                stringBuilder.append(smsMessages[i].getMessageBody());
            }
            String sender = smsMessages[0].getOriginatingAddress();

            sender = PhoneNumberUtils.formatNumber(smsMessages[0].getOriginatingAddress());

            String message = stringBuilder.toString();
            String newSender = "";
            boolean caughtFirstDash = false;
            for (char c : sender.toCharArray()) {
                if (c == '-' && !caughtFirstDash) {
                    caughtFirstDash = true;
                    newSender = newSender + ' ';
                } else {
                    newSender = newSender + c;
                }
            }
            sender = newSender;


            long numberID = Contact.getIdFromNumber(sender);
            Contact contact = new Contact(context, numberID);
            String key = null;
            SecurityContact sContact = null;
            if (SecurityContact.contactIdIsASecurityContact(contact.getId())) {
                sContact = new SecurityContact(contact.getId());
                key = sContact.getSessionKey();
            }


            //Handling check for encryption and decryption
            String fixed = null;
            boolean encrypted = false;

            if (message.contains(expireAllKeys) && key != null) {
                sContact.receivedExpireAllKeys();
            } else if (message.contains(expireKey) && key != null) {
                sContact.receivedExpireKey();
            } else if (message.contains(getPrepend()) && key != null) {
                fixed = "";
                encrypted = true;
                for (int j = getPrepend().length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
                fixed = MessageCipher.decrypt(fixed, key, key2);
                //fixed = messageCipher.decrypt(fixed, key1, key2);

            } else if (message.contains(prependDH)) {
                fixed = "";
                for (int j = prependDH.length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
            } else {
                fixed = message;
            }

            //Return the Message
            MyMessage msgObj = new MyMessage(numberID, fixed, false);
            msgObj.set_encrypted(encrypted);
            if (message.contains(prependDH)) {
                msgObj.setIsDHKey(true);
            }
            return msgObj;
            //Uncommenting the line below will prevent other receivers from getting this message.
            //abortBroadcast();

        }
        return null;
    }

    public static String getPrepend() {
        return prepend;
    }
}
