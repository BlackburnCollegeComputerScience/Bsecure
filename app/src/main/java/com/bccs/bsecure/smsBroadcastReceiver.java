package com.bccs.bsecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

//import org.apache.commons.codec.binary.Base64;

/*
 * Created by traci.kamp on 10/27/2014.
    Catches incoming SMS messages displays a Toast message
    containing sender phone number and message text.

 * Modified by traci.kamp on 11/3/2014.
    Now checks incoming SMS messages for a prepended sequence
    of characters ( -&*&- ) and performs decryption of the
    message if the characters are found (first removes the
    character sequence).

 * Modified by traci.kamp on 11/10/2014
    Design changes require application restructure. Changes
    to this file are not immediately apparent; seems
    that changes will likely be small and centered on
    efficiency rather than a major overhaul. If major
    changes are necessary, the cipher functions will likely
    be relocated to a specialized java file. Base64 jar
    files are still needed for this project.
 */

public class smsBroadcastReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "smsBroadcastReceiver";
    private static final String SMS_SENT = "android.provider.Telephony.SMS_SENT";
    private static final String ERROR = "Error: ";
    final SmsManager mySMSManager = SmsManager.getDefault();
    String phoneNumber;
    String key1 = "Bar12345Bar12345"; // 128 bit key
    String key2 = "ThisIsASecretKey";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(SMS_RECEIVED)) handleIncMessage(intent.getExtras(), context);
        else if (intent.getAction().equals(SMS_SENT)) sendSMS(intent.getExtras(), context);

    }

    void sendSMS(Bundle bundle, Context context) {
        phoneNumber = bundle.getString(Intent.EXTRA_PHONE_NUMBER);
        Log.i("info", "Outgoing Number: " + phoneNumber);
        context.sendBroadcast(new Intent("onNewMsgSend"));
    }

    private void addReceivedMessageToDatabase(myMessage message) {
        //dbHelper helper = new dbHelper();
    }

    void handleIncMessage(Bundle bundle, Context context) {

        if (bundle != null) {

            Object[] pdus = (Object[]) bundle.get("pdus");
            for (int i = 0; i < pdus.length; i++) {
                //extract message information
                SmsMessage currMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
                String sendingNum = currMessage.getDisplayOriginatingAddress();
                String message = currMessage.getDisplayMessageBody();
                System.out.println("Message before chop: " + message);

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

                //display a toast notification
                int duration = Toast.LENGTH_LONG;

                /*
                    Used the ternary operator <condition> ? true : false
                    for ease of switching out necessary messages
                    added 11.3.14
                 */
                String toastString = "sender: " + sendingNum + "; message: " +
                        (fixed == null ? message : fixed);
                Toast.makeText(context, toastString, duration).show();

                myMessage msgObj = new myMessage(sendingNum,
                        fixed == null ? message : fixed, false);

                addReceivedMessageToDatabase(msgObj);



                //print information to logcat
                //updated to use ternary on 11.3.14
                Log.i(TAG, "SENDER: " + sendingNum + "; Message: " +
                        (fixed == null ? message : fixed));


            }

        } else {
            //bundle was null, print an error in logcat
            Log.e(ERROR, "bundle was null");
        }
    }
}
