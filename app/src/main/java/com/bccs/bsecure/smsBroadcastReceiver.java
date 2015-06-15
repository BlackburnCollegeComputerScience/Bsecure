package com.bccs.bsecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

 * Modified by lucas.burdell on 6/5/2015.
    Message unpacking and decryption migrated to handleMessage file.
    Notifications are now built by this class. This class tracks
    the most recentNumber received and notifications will display the
    most recent message. Taping the notification brings user to the conversation
    of the most recently received message.

 * Modified by lucas.burdell on 6/12/2015
    Added handle for outgoing messages sent by other applications to appear in conversation as well.
    This is so if you send messages via the android messenger app it will appear in conversation in
    BSecure.
 */

public class smsBroadcastReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String SMS_SENT = "android.provider.Telephony.SMS_SENT";

    public static String recentNumber = "5556";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMS_RECEIVED)) handleIncomingMessage(intent, context);
        if (intent.getAction().equals(SMS_SENT)) handleOutgoingMessage(intent, context);
    }

    private void addReceivedMessageToDatabase(myMessage message, Context context) {
        dbHelper database = new dbHelper(context);
        database.addRecord(message);
        database.close();
    }

    public void handleOutgoingMessage(Intent intent, Context context) {
        myMessage msg = handleMessage.handleOutgoingMessage(intent);
        if (msg != null) {
            addReceivedMessageToDatabase(msg, context);
        }
    }

    public void handleIncomingMessage(Intent intent, Context context) {
        abortBroadcast();
        myMessage msg = handleMessage.handleIncomingMessage(intent, context);
        if (msg != null) {
            if (msg.is_encrypted() || msg.isDHKey()) abortBroadcast();
            if (!msg.isDHKey()) {
                addReceivedMessageToDatabase(msg, context);
                Intent receivedMSG = new Intent("com.bccs.bsecure.msgReceived");
                receivedMSG.putExtra("number", msg.get_number());
                recentNumber = msg.get_number();
                messageReceivedNotification.cancel(context); //cancel old message
                messageReceivedNotification.notify(context, msg.get_number(), msg.getBody());
                LocalBroadcastManager.getInstance(context).sendBroadcast(receivedMSG);
            } else {
                Intent receivedMSG = new Intent("com.bccs.bsecure.msgReceived_DH");
                receivedMSG.putExtra("body", msg.getBody());
                receivedMSG.putExtra("number", msg.get_number());
                LocalBroadcastManager.getInstance(context).sendBroadcast(receivedMSG);
            }
        }

    }

}
