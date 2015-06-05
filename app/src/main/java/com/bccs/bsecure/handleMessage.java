package com.bccs.bsecure;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.ArrayList;


/**
 * Created by lucas.burdell on 6/4/2015.
 * This file will handle sending outbound messages, which includes setting the header and calling
 * the cipher module for encryption.
 */
public class handleMessage {

    // TODO: Replace temporary keys with key pair associated with each contact
    private static final String key1 = "Bar12345Bar12345"; // 128 bit key
    private static final String key2 = "ThisIsASecretKey";


    private static final String prepend = "-&*&-"; // current message header

    /**
     * send method for sending messages to a number
     * @param number number to send to
     * @param msg message to send
     * @return myMessage object to be used by UI
     */
    public static myMessage send(String number, String msg) {
        System.out.println("Creating message object: ");
        SmsManager sms = SmsManager.getDefault();

        myMessage msgObj = new myMessage(number, msg, true);

        System.out.println(msgObj.toString());
        msg = messageCipher.encrypt(msg, key1, key2);
        String newMsg = prepend + msg;

        //new multipart text messages
        ArrayList<String> messages = sms.divideMessage(newMsg);
        sms.sendMultipartTextMessage(number, null, messages, null, null);

        System.out.println("Message sent: " + newMsg);
        return msgObj;
    }


    /**
     * Handle incoming message and return myMessage object to be added to the DB
     * @param bundle data bundle from an intent (intent.getExtras())
     * @return myMessage object to represent the message
     */
    public static myMessage handleIncomingMessage(Bundle bundle) {
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (int i = 0; i < pdus.length; i++) {

                //extract message information
                SmsMessage currMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
                String sendingNum = currMessage.getDisplayOriginatingAddress();
                String message = currMessage.getDisplayMessageBody();
                //System.out.println("Message received from " + sendingNum);
                //System.out.println("Message before chop: " + message);

                //Handling chars to remove if decrypt needed
                //added 11.3.14
                String fixed = null;
                if (message.contains("-&*&-")) {
                    fixed = "";
                    for (int j = 5; j < message.length(); j++) {
                        fixed += message.charAt(j);
                    }
                    fixed = messageCipher.decrypt(fixed, key1, key2);
                }

                /*
                    Used the ternary operator <condition> ? true : false
                    for ease of switching out necessary messages
                    added 11.3.14
                 */

                myMessage msgObj = new myMessage(sendingNum,
                        fixed == null ? message : fixed, false);

                return msgObj;
            }

        } else {
            //bundle was null, print an error in logcat

            return null;
        }
        return null;
    }

    public static String getPrepend() {
        return prepend;
    }
}
