package com.bccs.bsecure;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.ArrayList;


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

    //These temp keys were generated with Diffie-Hellman key exchange. The second key (the IV for the cipher)
    //Is a SHA-256 hash of the key1, a Diffie-Hellman generated key.
    //private static final String key1 = "524F0D82AFA4779CB7A55358798117A88A90549730659C0778CEBE5BAD7FDD77";
    //private static final String key2 = "5155F276EB66C9D56D3335A3B7150E621CA012EF6A660834D90EB67341BA36C6";


    private static final String prepend = "-&*&-"; // current message header
    private static final String prependDH = "*-&-*"; // header for DH key exchanges

    /**
     * send method for sending messages to a number
     * @param number number to send to
     * @param msg message to send
     * @return myMessage object to be used by UI
     */
    public static myMessage send(String number, String msg, Context context) {
        return send(number, msg, context, false);
    }


    public static myMessage send(String number, String msg, Context context, boolean isDH) {
        if (!isDH) {
            System.out.println("Creating message object: ");
            SmsManager sms = SmsManager.getDefault();

            myMessage msgObj = new myMessage(number, msg, true);
            System.out.println(msgObj.toString());

            //PULL FROM DB
            dbHelper helper = new dbHelper(context);
            String[] keys = helper.getKey(number);
            helper.close();
            if (keys != null) {
                msg = messageCipher.encrypt(msg, keys[0], keys[1]);
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

            sms.sendMultipartTextMessage(number, null, messages, sentIntents, deliveryIntents);
            msgObj.set_encrypted(keys != null);

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
            myMessage msgObj = new myMessage(number, msg, true);
            msgObj.setIsDHKey(true);
            sms.sendMultipartTextMessage(number, null, messages, null, null);
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
            myMessage msgObj = new myMessage(sender, message);
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
                stringBuilder.append(smsMessages[i].getMessageBody().toString());
            }
            String sender = smsMessages[0].getOriginatingAddress();
            String message = stringBuilder.toString();
            System.out.println("Here is the message you sent: " + message);
            //Handling check for encryption and decryption
            String fixed = null;
            boolean encrypted = false;
            dbHelper helper = new dbHelper(context);
            String[] keys = helper.getKey(sender);
            helper.close();
            if (message.contains(getPrepend()) && keys != null) {
                fixed = "";
                encrypted = true;
                for (int j = getPrepend().length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
                fixed = messageCipher.decrypt(fixed, keys[0], keys[1]);
                //fixed = messageCipher.decrypt(fixed, key1, key2);

            } else if (message.contains(prependDH)) {
                fixed = "";
                for (int j = prependDH.length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
            } else { fixed = message; }

            //Return the Message
            myMessage msgObj = new myMessage(sender, fixed, false);
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
