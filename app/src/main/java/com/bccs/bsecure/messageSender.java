package com.bccs.bsecure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Base64;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//import org.apache.commons.codec.binary.Base64;

/*

 * Created by traci.kamp on 10/15/2014.

 * Modified by traci.kamp on 11/10/2014 -> renamed to messageSender
     as it is the appropriate name for the function it performs.
     View should be replaced with convo_with_bubbles xml files
     when functionality is appropriate for the view itself.

     Added dbHelper functionality.

 * Modified by traci.kamp on 11/13/2014.
    Removed getNumber() method as it has been verified that the
    correct phone number for the selected contact is being
    passed to this Activity from the previous Activity.

    Removed dead layout code.

    Hooked up the 'send' button. CAUTION - THE METHOD IS
    HARD-CODED TO SEND TO THE EMULATOR!! MODIFY THIS WHEN
    APP IS AT COMPLETED STAGE.

    Added Activity lifecycle methods.

 * Modified by traci.kamp on 11/17/2014.
    Added readConvo() method to read the conversation from the
    outbox database. It is returned as an arraylist<string>
    and then the contents are printed for testing purposes
    only. Verified working correctly.

     TODO: Complete integration of new layout view
        TODO: Sub-item: BUBBLES!!!
        TODO: Sub-item: READ INBOX!!!
        TODO: DISPLAY ABOVE CONTENTS!!!

     TODO: Figure out how to open database in Lifecycle methods...
        TODO: idea - create a returned db in the helper method, store, reopen?

 * Modified by traci.kamp on 11/28/2014.
    Followed some instruction from chat example activity.
    Example did not display the chat bubbles as intended.
    Clearly code is missing but I am unsure about where
    it is missing. Any unadded code from the example is
    purely functionality based; the bubbles should be
    displaying from what I have gathered and they are
    not.

 * Modified by traci.kamp on 12/1/2014.
    Checked over errors from AS and realized that
    I did not need the chatArrayAdapter class from
    the example I found and that it was actually
    hindering progress. I removed the file and
    used a previous ArrayAdapter implementation
    to successfully implement one in this file.
    Bubbles now display; however, the bubble and
    gravity are incorrect for the kind of message
    it contains.

    UPDATE: Switched the bubble to the correct one.
    Gravity is still incorrect.

    UPDATE: It appears that the example does the dynamic
    handling of the bubbles in the chatArrayAdapter class,
    which I have removed from this project. A find-usages
    search for the method yielded no results. The method
    is unused in the example project. I am concerned
    that the example project is fraudulent.

     TODO: Make bubbles dynamic based on "left" status
     TODO: Make bubbles actually go to the right...
     TODO: Combine ArrayLists and sort by recency

 */

public class messageSender extends Activity {

    EditText typeMessage;
    Button send;
    String number, name;
    myMessage message;
    String key1 = "Bar12345Bar12345"; // 128 bit key
    String key2 = "ThisIsASecretKey";
    String prepend = "-&*&-"; // current message header
    dbHelper helper;
    boolean dbActive = false;
    boolean receiversRegistered = false; // track if receivers are registered or not
    smsBroadcastReceiver onNewMsg = new smsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
                It has been determined that this method is not
                executed. not sure if it is necessary to have
                here or if it is taking up space...
            */
        }
    };

    smsBroadcastReceiver onNewMsgSend = new smsBroadcastReceiver();
    private ListView listView;
    private ArrayAdapter chatAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the user view
        setContentView(R.layout.convo_with_bubbles);

        // Initialize variables from intent information passed
        if (getIntent().hasExtra("name") && getIntent().hasExtra("number")) {
            name = getIntent().getStringExtra("name");
            number = getIntent().getStringExtra("number");
        } else {
            name = "Unknown";
            number = "Unknown";
        }

        // Use Intent info to title the activity
        setTitle(name + " " + number);

        // Initialize database variables
        helper = new dbHelper(this);
        dbActive = true;
        ArrayList<String> conversation = readConvo(number); // outbox history

        // Initialize layout variables
        typeMessage = (EditText) findViewById(R.id.chatText); // Layout file from example
        listView = (ListView) findViewById(R.id.listView1); // From example
        send = (Button) findViewById(R.id.buttonSend);

        // Set up array adapter - From example in listContacts
        chatAdapter = new ArrayAdapter<String>
                (getApplicationContext(), R.layout.singlemessageoutbox, conversation);

        // Give the listView an adapter - From example
        listView.setAdapter(chatAdapter);
        final EditText number = (EditText) findViewById(R.id.editText);
        // Create the on-click listener for the send button
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO: CAREFUL! THIS IS HARD-CODED TO SEND TO EMULATOR!
                String recipientNo = number.getText().toString();
                String message = typeMessage.getText().toString();
                typeMessage.setText("");

                if (recipientNo.length() > 0 && message.length() > 0) {
                    sendMsg(recipientNo, message);
                }
            }
        });

        // The following was added from ChatBubble example.
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatAdapter);
        // Scrolls to the bottom in the event of data change.
        chatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatAdapter.getCount() - 1);
            }
        });

        registerReceiver(onNewMsg, new IntentFilter("onNewMsg"));
        registerReceiver(onNewMsgSend, new IntentFilter("onNewMsgSend"));
        receiversRegistered = true;

    }

    protected void onStart() {
        System.out.println("Sender onStart");

        if (!receiversRegistered) {
            registerReceiver(onNewMsg, new IntentFilter("onNewMsg"));
            registerReceiver(onNewMsgSend, new IntentFilter("onNewMsgSend"));
            receiversRegistered = true;
        }
        //open database here
        super.onStart();
    }

    protected void onResume() {
        System.out.println("Sender onResume");

        if (!receiversRegistered) {
            registerReceiver(onNewMsg, new IntentFilter("onNewMsg"));
            registerReceiver(onNewMsgSend, new IntentFilter("onNewMsgSend"));
            receiversRegistered = true;
        }
        //open database here
        super.onResume();
    }

    // Missing onFreeze() ??????
    // Missing onRestart() ??????

    protected void onPause() {
        System.out.println("Sender onPause");

        if (receiversRegistered) {
            unregisterReceiver(onNewMsg);
            unregisterReceiver(onNewMsgSend);
            receiversRegistered = false;
        }

        //old code for preventing crash when pressing back button
        //try { unregisterReceiver(onNewMsg); } catch (IllegalArgumentException iae) { };
        //try { unregisterReceiver(onNewMsgSend); } catch (IllegalArgumentException iae) { };

        helper.close();
        super.onPause();
    }

    protected void onStop() {
        System.out.println("Sender onStop");
        if (receiversRegistered) {
            unregisterReceiver(onNewMsg);
            unregisterReceiver(onNewMsgSend);
            receiversRegistered = false;
        }
        helper.close();
        super.onStop();
    }

    protected void onDestroy() {
        System.out.println("Sender onDestroy");
        if (receiversRegistered) {
            unregisterReceiver(onNewMsg);
            unregisterReceiver(onNewMsgSend);
            receiversRegistered = false;
        }
        helper.close();
        super.onDestroy();
    }


    private void sendMsg(String no, String msg) {
        System.out.println("Creating message object: ");
        SmsManager sms = SmsManager.getDefault();



        final EditText number = (EditText) findViewById(R.id.editText);
        no = number.getText().toString();
        /*
            The name field is being filled with "emulator" for now but will need to be changed
            when the app is running properly so it reflects the appropriate contact information.
          */
        myMessage msgObj = new myMessage(no, no, msg);
        helper.addRecord(msgObj);
        System.out.println(msgObj.toString());
        msg = encrypt(msg);
        String newMsg = prepend + msg;

        //new multipart text test
        ArrayList<String> messages = sms.divideMessage(newMsg);
        sms.sendMultipartTextMessage(no, null, messages, null, null);


        //sms.sendTextMessage(no, null, newMsg, null, null);

        //I cannot remember why the below line is commented out.
        //sms.sendTextMessage(no, null, msg, null, null);
        System.out.println("Message sent: " + newMsg);
    }

    private ArrayList<String> readConvo(String pNum) {
        return helper.getConversation(pNum);
    }


    //Dr. Coogan's encrypt method, some variations made

    public String encrypt(String value) {

        try {
            IvParameterSpec iv = new IvParameterSpec(key2.getBytes("UTF-8"));

            SecretKeySpec skeySpec = new SecretKeySpec(key1.getBytes("UTF-8"),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes());

            System.out.println("encrypted string:"
                    + Base64.encodeToString(encrypted, 0));
            return Base64.encodeToString(encrypted, 0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
