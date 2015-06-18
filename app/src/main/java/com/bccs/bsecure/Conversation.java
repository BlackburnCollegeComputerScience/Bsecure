package com.bccs.bsecure;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/** Created by lucas.burdell 6/4/2015.
 * Handles conversation between user and another phone. Sent messages are
 * displayed using the "sentmessage.xml" layout and received messages are
 * displayed using the "receivedmessage.xml" layout.
 *
 * Modified by lucas.burdell 6/12/2015.
 * Now checks whether a message was encrypted or not when it was sent/received.
 * Sent and received messages that were not encrypted are displayed using the
 * "sentmessagenoenc.xml" and "receivedmessagenoenc.xml" layouts.
 */

public class Conversation extends ActionBarActivity implements WipeConversationDialog.WipeConversationDialogListener {
    String currentNumber;
    boolean receiversRegistered = false; // track if receivers are registered or not
    ArrayList<String> conversation;
    EditText typeMessage;
    Button send;


    private ListView listView;
    private chatAdapter chatAdapter;


    //Receiver for detecting if a message was received by the main smsBroadcastReceiver.
    //Subclassed the smsBroadcastReceiver rather than BroadcastReceiver
    //for some convenience that is no longer apparent.
    smsBroadcastReceiver onNewMsg = new smsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("number").equals(currentNumber)) {
                updateConvo();
            }
        }
    };

    //Filter to catch broadcasts from the main smsBroadcastReceiver when an SMS is received.
    IntentFilter onNewMsgFilter = new IntentFilter("com.bccs.bsecure.msgReceived");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //If convo was opened from the main view, it will have a number value in the intent
        //currentNumber variable is used when loading messages from the DB.
        if (getIntent().hasExtra("number")) {
            currentNumber = getIntent().getStringExtra("number");
            //Cancel any notifications for this number
            if (currentNumber == smsBroadcastReceiver.recentNumber) messageReceivedNotification.cancel(this);
        } else {
            //If it was not opened from the main view, use the most recent number an SMS
            //was received from.
            currentNumber = smsBroadcastReceiver.recentNumber;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        setTitle(currentNumber);

        conversation = new ArrayList<>();
        typeMessage = (EditText) findViewById(R.id.messageEditText);
        listView = (ListView) findViewById(R.id.listView);
        send = (Button) findViewById(R.id.sendButton);


        chatAdapter = new chatAdapter();
        listView.setAdapter(chatAdapter);
        // Create the on-click listener for the send button
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = typeMessage.getText().toString();
                typeMessage.setText("");

                if (currentNumber.length() > 0 && message.length() > 0) {
                    myMessage msgObj = handleMessage.send(currentNumber, message, getApplicationContext());
                    dbHelper helper = new dbHelper(getBaseContext());
                    helper.addRecord(msgObj);
                    helper.close();
                    updateConvo();
                }
            }
        });


        // Scrolls to the bottom in the event of data change.
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        chatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatAdapter.getCount());
            }
        });

        ((Button) findViewById(R.id.wipeConversation)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                WipeConversationDialog dialog = new WipeConversationDialog();
                dialog.show(getFragmentManager(), "conversation_option");
            }
        });

        //Register our receiver in the LocalBroadcastManager.
        //NOTE this is different from calling registerReceiver from the contextWrapper.
        LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
        receiversRegistered = true;

        updateConvo();
    }


    /*
                    dbHelper helper = new dbHelper(Conversation.this);
                helper.clearMessagesFromNumber(currentNumber);
                helper.close();
                Intent intent = new Intent(Conversation.this, Main.class);
                startActivity(intent);
     */


    /**
     * This method updates the chat with new messages and will filter out duplicates.
     */
    private void updateConvo() {
        System.out.println("Updating conversation " + currentNumber);

        dbHelper helper = new dbHelper(this);
        ArrayList<myMessage> checkConversation = helper.getConversationMessages(currentNumber);
        helper.close();
        ArrayList<myMessage> oldConversation = chatAdapter.getChatArray();
        ArrayList<myMessage> oldMessages = new ArrayList<>();
        for (int i = 0; i < oldConversation.size(); i++) {
            oldMessages.add(checkConversation.get(i));
        }

        for (myMessage m : oldMessages) {
            checkConversation.remove(m);
        }


        for (myMessage m : checkConversation) {
            chatAdapter.addItem(m, m.getSent(), m.is_encrypted());
        }
    }

    protected void onStart() {
        System.out.println("Sender onStart");
        if (!receiversRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiversRegistered = true;
        }
        super.onStart();
    }

    protected void onResume() {
        System.out.println("Sender onResume");
        if (!receiversRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiversRegistered = true;
        }
        super.onResume();
    }

    protected void onPause() {
        System.out.println("Sender onPause");
        if (receiversRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiversRegistered = false;
        }

        super.onPause();
    }

    protected void onStop() {
        System.out.println("Sender onStop");
        if (receiversRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiversRegistered = false;
        }
        super.onStop();
    }

    protected void onDestroy() {
        System.out.println("Sender onDestroy");
        if (receiversRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiversRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public void onOKPressed(DialogFragment dialog) {
        dbHelper helper = new dbHelper(Conversation.this);
        helper.clearMessagesFromNumber(currentNumber);
        helper.close();
        dialog.dismiss();
        Intent intent = new Intent(Conversation.this, Main.class);
        startActivity(intent);
    }

    @Override
    public void onCancelPressed(DialogFragment dialog) {
        dialog.dismiss();
    }


    // Custom Adapter to display the differing layouts for chat.
    private class chatAdapter extends BaseAdapter {

        //Possibility to add more types
        private static final int SENT = 0;
        private static final int RECEIVED = 1;
        private static final int SENT_NOENC = 2;
        private static final int RECEIVED_NOENC = 3;
        private static final int MAX_TYPES = 4;

        private ArrayList<myMessage> chatArray = new ArrayList<>();
        private LayoutInflater inflater;
        private ArrayList<Integer> typeArray = new ArrayList<>();

        public chatAdapter() {
            //create inflater that will hold the chat boxes
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(myMessage message, boolean isSent, boolean isEncrypted) {
            chatArray.add(message);
            int type;
            if (!isEncrypted) {
                type = isSent ? SENT_NOENC : RECEIVED_NOENC;
            } else {
                type = isSent ? SENT : RECEIVED;
            }
            typeArray.add(type);
            notifyDataSetChanged();
        }


        public ArrayList<myMessage> getChatArray() {
            ArrayList<myMessage> newChat = new ArrayList<>();
            for (myMessage m : chatArray) {
                newChat.add(m);
            }
            return newChat;
        }


        @Override
        public int getItemViewType(int position) {
            return typeArray.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return MAX_TYPES;
        }

        @Override
        public int getCount() {
            return chatArray.size();
        }

        @Override
        public myMessage getItem(int position) {
            return chatArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout holder = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    case SENT:
                        convertView = inflater.inflate(R.layout.sentmessage, null);
                        holder = (FrameLayout) ((LinearLayout) convertView).getChildAt(0);
                        break;
                    case RECEIVED:
                        convertView = inflater.inflate(R.layout.receivedmessage, null);
                        holder = (FrameLayout) ((LinearLayout) convertView).getChildAt(0);
                        break;
                    case SENT_NOENC:
                        convertView = inflater.inflate(R.layout.sentmessagenoenc, null);
                        holder = (FrameLayout) ((LinearLayout) convertView).getChildAt(0);
                        break;
                    case RECEIVED_NOENC:
                        convertView = inflater.inflate(R.layout.receivedmessagenoenc, null);
                        holder = (FrameLayout) ((LinearLayout) convertView).getChildAt(0);
                        break;
                }
            } else {
                holder = (FrameLayout) ((LinearLayout) convertView).getChildAt(0);
            }
            myMessage msg = getItem(position);
            TextView text =(TextView) ((LinearLayout) holder.getChildAt(0)).getChildAt(0);
            TextView date = (TextView) holder.getChildAt(1);
            String dateText = (new SimpleDateFormat("h:mm a, EEE, MMM d, ''yy")).format(
                    new Date(msg.get_time()));
            text.setText(msg.getBody());
            date.setText(dateText);
            return convertView;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_conversation, menu);
        getMenuInflater().inflate(R.menu.menu_conservative, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_new:
                openNewMessage();
                return true;
            case R.id.action_contacts:
                openContacts();
                return true;
            case R.id.action_nfc:
                //Checks if the device supports NFC. If not opens the NoNFC activity to communicate
                //through test messaging.
                NfcManager nfcManager = (NfcManager) getApplicationContext().getSystemService(Context.NFC_SERVICE);
                NfcAdapter nfcAdapter = nfcManager.getDefaultAdapter();
                if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                    openNFC();
                } else {
                    openNoNFC();
                }
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_bugReport:
                openBugReport();
                return true;
            case R.id.action_about:
                openAbout();
                return true;
            default:
                openMain();
                return true;
        }
    }

    public void openMain() {
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
    }

    public void openNewMessage(){
        Intent intent = new Intent(this, CreateMessage.class);
        startActivity(intent);
    }
    public void openContacts(){
        Intent intent = new Intent(this, Contacts.class);
        startActivity(intent);
    }
    public void openNFC(){
        Intent intent = new Intent(this, NFC.class);
        startActivity(intent);
    }

    public void openNoNFC() {
        Intent intent = new Intent(this, SMSExchange.class);
        startActivity(intent);
    }
    public void openSettings(){
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }
    public void openBugReport(){
        Intent intent = new Intent(this, BugReport.class);
        startActivity(intent);
    }
    public void openAbout(){
        Intent intent = new Intent(this, About.class);
        startActivity(intent);
    }

}
