package com.bccs.bsecure;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.ArrayList;


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
 * Created by lucas.burdell on 6/4/2015.
 * This file will handle sending outbound messages, which includes setting the header and calling
 * the cipher module for encryption.
 *
 * Modified by shane.nalezyty on 6/9/2015
 * Edited the Sms receiving method to recognise sms split into parts and append them back together
 * before decryption.
 *
 * Modified by lucas.burdell on 6/12/2015
 * Changed original temporary keys to a Diffie-Hellman generated key and an SHA-256 hash of the
 * that key. Both were converted to hexadecimal represented in a string.
 * Added very basic functionality to database to store and retrieve DH keys
 *
 *
 */
public class handleMessage {

    //These are temp keys
    //private static final String key1 = "524F0D82AFA4779CB7A55358798117A88A90549730659C0778CEBE5BAD7FDD77";
    private static final String key2 = "NVzTTl4v9Z0jOL2qyNuOFodEhN8ArwZIG4RJ9g8zQlI=";


    private static final String prepend = "-&*&-"; // current message header
    private static final String prependDH = "*-&-*"; // header for DH key exchanges

    /**
     * send method for sending messages to a number
     * @param contactid the contactid to send to
     * @param msg message to send
     * @return myMessage object to be used by UI
     */
    public static myMessage send(int contactid, String msg, Context context) {
        return send(contactid, msg, context, false);
    }


    public static myMessage send(int contactid, String msg, Context context, boolean isDH) {
        Contact contact = new Contact(contactid);
        if (!isDH) {
            System.out.println("Creating message object: ");
            SmsManager sms = SmsManager.getDefault();

            myMessage msgObj = new myMessage(contactid, msg, true);
            System.out.println(msgObj.toString());

            //PULL FROM DB
            String key = null;
            SecurityContact sContact = null;
            if (SecurityContact.contactIdIsASecurityContact(contactid)) {
                sContact = new SecurityContact(contactid);
                key = sContact.getSessionKey();
            }

            if (key != null) {
                msg = messageCipher.encrypt(msg, key, key2);
                msg = getPrepend() + msg;
            }

            //new multipart text messages
            ArrayList<String> messages = sms.divideMessage(msg);
            int numberOfParts = messages.size();
            System.out.println(numberOfParts);

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

            System.out.println("Message sent: " + msg);
            return msgObj;
        } else {
            System.out.println("Creating message object: ");
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> messages = sms.divideMessage(msg);
            int numberOfParts = messages.size();
            System.out.println(numberOfParts);
            for (String s : messages) {
                System.out.println(s.length());
            }
            System.out.printf("Total: " + msg.length());
            myMessage msgObj = new myMessage(contactid, msg, true);
            msgObj.setIsDHKey(true);
            sms.sendMultipartTextMessage(contact.getNumber(), null, messages, null, null);
            System.out.println("Message sent: " + msg);
            return msgObj;
        }
    }


    /**
     * Handle outgoing messages not sent by this application
     * @param intent Intent from received message
     * @return myMessage object to represent message
     */
    public static myMessage handleOutgoingMessage(Intent intent) {
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
            System.out.println(pdus.length);
            for (int i = 0; i < pdus.length; i++) {
                smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                stringBuilder.append(smsMessages[i].getMessageBody());
            }
            String sender = smsMessages[0].getOriginatingAddress();
            String message = stringBuilder.toString();
            System.out.println("Here is the message you sent: " + message);
            myMessage msgObj = new myMessage(Contact.getIdFromNumber(sender), message);
            msgObj.set_encrypted(false);
            return msgObj;
        }
        return null;
    }

    /**
     * Handle incoming message and return myMessage object to be added to the DB
     * @param intent Intent from the received message
     * @return myMessage object to represent the message
     */
    public static myMessage handleIncomingMessage(Intent intent, Context context) {
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
            System.out.println(pdus.length);
            for (int i = 0; i < pdus.length; i++) {
                smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                stringBuilder.append(smsMessages[i].getMessageBody());
            }
            String sender = smsMessages[0].getOriginatingAddress();
            String message = stringBuilder.toString();
            System.out.println("Here is the message you sent: " + message);


            Contact contact = new Contact(Contact.getIdFromNumber(sender));
            String key = null;
            SecurityContact sContact = null;
            if (SecurityContact.contactIdIsASecurityContact(contact.getId())) {
                sContact = new SecurityContact(contact.getId());
                key = sContact.getSessionKey();
            }


            //Handling check for encryption and decryption
            String fixed = null;
            boolean encrypted = false;

            if (message.contains(getPrepend()) && key != null) {
                fixed = "";
                encrypted = true;
                for (int j = getPrepend().length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
                fixed = messageCipher.decrypt(fixed, key, key2);
                //fixed = messageCipher.decrypt(fixed, key1, key2);

            } else if (message.contains(prependDH)) {
                fixed = "";
                for (int j = prependDH.length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
            } else { fixed = message; }

            //Return the Message
            myMessage msgObj = new myMessage(contact.getId(), fixed, false);
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
