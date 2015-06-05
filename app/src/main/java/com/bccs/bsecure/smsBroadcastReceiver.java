package com.bccs.bsecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

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

    public static String recentNumber = "5556";
    @Override
    public void onReceive(Context context, Intent intent) {
        //System.out.println("RECEIVED");
        if (intent.getAction().equals(SMS_RECEIVED)) handleIncomingMessage(intent.getExtras(), context);
    }

    private void addReceivedMessageToDatabase(myMessage message, Context context) {
        dbHelper database = new dbHelper(context);
        database.addRecord(message);
        database.close();
    }

    public void handleIncomingMessage(Bundle bundle, Context context) {
        myMessage msg = sendMessage.handleIncomingMessage(bundle);
        addReceivedMessageToDatabase(msg, context);
        Intent receivedMSG = new Intent("com.bccs.bsecure.msgReceived");
        receivedMSG.putExtra("number", msg.get_number());
        recentNumber = msg.get_number();
        messageReceivedNotification.notify(context, msg.get_number(), msg.getBody());
        //receivedMSG.putExtra("body", msg.getBody());
        LocalBroadcastManager.getInstance(context).sendBroadcast(receivedMSG);
    }

}
