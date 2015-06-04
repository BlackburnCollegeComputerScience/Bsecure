package com.bccs.bsecure;

import android.telephony.SmsManager;

import java.util.ArrayList;


/**
 * Created by lucas.burdell on 6/4/2015.
 * This file will handle sending outbound messages, which includes setting the header and calling
 * the cipher module for encryption.
 */
public class sendMessage {

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

    public static String getPrepend() {
        return prepend;
    }
}
