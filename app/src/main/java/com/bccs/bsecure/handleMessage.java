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
 */
public class handleMessage {

    // TODO: Replace temporary keys with key pair associated with each contact
    private static final String key1 = "Bar12345Bar12345"; // 128 bit key
    private static final String key2 = "ThisIsASecretKey";


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

    public static ArrayList<String> safeDivide(String msg) {
        ArrayList<String> list = new ArrayList<>();
        if (msg.length() + prepend.length() < 160) {
            list.add(msg);
            return list;
        } else {
            System.out.println("dividing message:");
            System.out.println(msg.length());
            int total = msg.length() + (((int) ((msg.length() / 160.) + 0.5)) * prepend.length());
            int msgTotal = msg.length();
            int timesToDivide = (int) ((total / 160.) + 0.5);
            System.out.println("Total: " + total);
            System.out.println("Division time: " + timesToDivide);
            String msgToCut = msg;
            for (int i = 0; i < timesToDivide; i++) {
                if (msgTotal + prepend.length()> 160) {
                    list.add(prepend + msg.substring(0, 160 - prepend.length()));
                    msgToCut = msgToCut.substring(160 - prepend.length() + 1);
                    total = total - 160;
                    msgTotal = msgTotal - (160 - prepend.length());
                } else {
                    list.add(prepend + msgToCut);
                    return list;
                }
            }
            return list; //this should never be reached
        }
    }

    public static myMessage send(String number, String msg, Context context, boolean isDH) {
        if (!isDH) {
            System.out.println("Creating message object: ");
            SmsManager sms = SmsManager.getDefault();

            myMessage msgObj = new myMessage(number, msg, true);
            System.out.println(msgObj.toString());

            //        msg = messageCipher.encrypt(msg, key1, key2);
            //        String newMsg = getPrepend() + msg;

            //new multipart text messages
            ArrayList<String> messages = sms.divideMessage(msg);
            int numberOfParts = messages.size();
            System.out.println(numberOfParts);

            ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

            Intent mSendIntent = new Intent("CTS_SMS_SEND_ACTION");
            Intent mDeliveryIntent = new Intent("CTS_SMS_DELIVERY_ACTION");

            for (int i = 0; i < numberOfParts; i++) {
                sentIntents.add(PendingIntent.getBroadcast(context, 0, mSendIntent, 0));
                deliveryIntents.add(PendingIntent.getBroadcast(context, 0, mDeliveryIntent, 0));
            }

            sms.sendMultipartTextMessage(number, null, messages, sentIntents, deliveryIntents);


            System.out.println("Message sent: " + msg);
            return msgObj;
        } else {
            System.out.println("Creating message object: ");
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> messages = safeDivide(msg);
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
     * Handle incoming message and return myMessage object to be added to the DB
     * @param intent Intent from the received message
     * @return myMessage object to represent the message
     */
    public static myMessage handleIncomingMessage(Intent intent) {
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
            if (message.contains(getPrepend())) {
                fixed = "";
                for (int j = getPrepend().length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
                fixed = messageCipher.decrypt(fixed, key1, key2);
            } else if (message.contains(prependDH)) {
                fixed = "";
                for (int j = prependDH.length(); j < message.length(); j++) {
                    fixed += message.charAt(j);
                }
            } else { fixed = message; }

            //Return the Message
            myMessage msgObj = new myMessage(sender, fixed, false);
            if (message.contains(prependDH)) {
                msgObj.setIsDHKey(true);
            }
            return msgObj;
            //Uncommenting the line below will prevent other receivers from getting this message.
            //abortBroadcast()

        }
        return null;
    }

    public static String getPrepend() {
        return prepend;
    }
}
