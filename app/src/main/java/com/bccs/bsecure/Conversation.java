package com.bccs.bsecure;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/** Created by lucas.burdell 6/4/2015.
 * Handles conversation between user and another phone. Sent messages are
 * displayed using the "sentmessage.xml" layout and received messages are
 * displayed using the "receivedmessage.xml" layout.
 */

public class Conversation extends ActionBarActivity {
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
                // TODO: CAREFUL! THIS IS HARD-CODED TO SEND TO EMULATOR!
                String message = typeMessage.getText().toString();
                typeMessage.setText("");

                if (currentNumber.length() > 0 && message.length() > 0) {
                    myMessage msgObj = handleMessage.send(currentNumber, message);
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

        //Register our receiver in the LocalBroadcastManager.
        //NOTE this is different from calling registerReceiver from the contextWrapper.
        LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
        receiversRegistered = true;

        updateConvo();
    }

    /**
     * This method updates the chat with new messages and will filter out duplicates.
     * TODO: Fix filtering out similar messages that aren't necessarily duplicates
     * (for example if someone says "ok" in the same conversation the second "ok" will never
     * appear no matter the difference in time or sequence it appeared).
     */
    private void updateConvo() {
        System.out.println("Updating conversation " + currentNumber);

        //Not sure whether this snippet runs on the UI thread or an event thread
        dbHelper helper = new dbHelper(this);
        ArrayList<myMessage> checkConversation = helper.getConversationMessages(currentNumber);
        helper.close();
        ArrayList<String> oldConversation = chatAdapter.getChatArray();
        ArrayList<myMessage> oldMessages = new ArrayList<>();
        for (myMessage m : checkConversation) {
            if (oldConversation.contains(m.getBody())) {
                oldMessages.add(m);
            }
        }

        for (myMessage m : oldMessages) {
            checkConversation.remove(m);
        }


        for (myMessage m : checkConversation) {
            chatAdapter.addItem(m.getBody(), m.getSent());
        }

        /*
        final ArrayList<myMessage> newConversation = checkConversation;
        runOnUiThread(new Runnable() {
            public void run() {
                for (myMessage m : newConversation) {
                    chatAdapter.addItem(m.getBody(), m.getSent());
                }
            }
        });

        */
    }

    protected void onStart() {
        System.out.println("Sender onStart");

        if (!receiversRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            //registerReceiver(onNewMsg, onNewMsgFilter);
//            registerReceiver(onNewMsgSend, new IntentFilter("onNewMsgSend"));
            receiversRegistered = true;
        }
        //open database here
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




    // Custom Adapter to display the differing layouts for chat.
    // TODO: Come back and rewrite code. It is messy.

    private class chatAdapter extends BaseAdapter {

        //Possibility to add more types
        private static final int SENT = 0;
        private static final int RECEIVED = 1;
        private static final int MAX_TYPES = RECEIVED + 1;

        private ArrayList<String> chatArray = new ArrayList<>();
        private LayoutInflater inflater;
        private ArrayList<Integer> sentArray = new ArrayList<>();

        public chatAdapter() {
            //create inflater that will hold the chat boxes
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(String text, boolean isSent) {
            chatArray.add(text);
            if (isSent) {
                sentArray.add(chatArray.size() - 1);
            }
            notifyDataSetChanged();
        }


        public ArrayList<String> getChatArray() {
            ArrayList<String> newChat = new ArrayList<>();
            for (String s : chatArray) {
                newChat.add(s);
            }
            return newChat;
        }


        @Override
        public int getItemViewType(int position) {
            return sentArray.contains(position) ? SENT : RECEIVED;
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
        public String getItem(int position) {
            return chatArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView holder = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    //Possibility to add more types
                    case SENT:
                        convertView = inflater.inflate(R.layout.sentmessage, null);
                        holder = (TextView) ((LinearLayout) convertView).getChildAt(0);
                        break;
                    case RECEIVED:
                        convertView = inflater.inflate(R.layout.receivedmessage, null);
                        holder = (TextView) ((LinearLayout) convertView).getChildAt(0);
                        break;
                }
            } else {
                holder = (TextView) ((LinearLayout) convertView).getChildAt(0);
            }
            holder.setText(chatArray.get(position));
            return convertView;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        onBackPressed();
        return true;
    }
}
