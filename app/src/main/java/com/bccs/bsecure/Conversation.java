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


public class Conversation extends ActionBarActivity {
    String currentNumber;
    boolean dbActive = false;
    boolean receiversRegistered = false; // track if receivers are registered or not
    ArrayList<String> conversation;
    EditText typeMessage;
    Button send;
    int currentChatIndex = 0;


    private ListView listView;
    private chatAdapter chatAdapter;


    smsBroadcastReceiver onNewMsg = new smsBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("number").equals(currentNumber)) {
                updateConvo();
            }
            /*
            myMessage msgObj =  sendMessage.handleIncomingMessage(intent.getExtras());
            if (msgObj.get_number().equals(currentNumber)) {
                updateConvo();
            } else {
                Toast.makeText(getApplicationContext(), msgObj.get_number() + ": " + msgObj.getBody(),
                    Toast.LENGTH_LONG).show();
            }
            */
        }
    };

    IntentFilter onNewMsgFilter = new IntentFilter("com.bccs.bsecure.msgReceived");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra("number")) {
            currentNumber = getIntent().getStringExtra("number");
            if (currentNumber == smsBroadcastReceiver.recentNumber) messageReceivedNotification.cancel(this);
        } else {
            //System.out.println("Got intent but filter does not match!");
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
                    myMessage msgObj = sendMessage.send(currentNumber, message);
                    dbHelper helper = new dbHelper(getBaseContext());
                    helper.addRecord(msgObj);
                    helper.close();
                    updateConvo();
                }
            }
        });

        // The following was added from ChatBubble example.

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        // Scrolls to the bottom in the event of data change.
        chatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatAdapter.getCount());
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
        //registerReceiver(onNewMsg, onNewMsgFilter);
//        registerReceiver(onNewMsgSend, new IntentFilter("onNewMsgSend"));
        receiversRegistered = true;
        updateConvo();
    }

    private void updateConvo() {
        System.out.println("Updating conversation " + currentNumber);


        final Context leakage = this;
        dbHelper helper = new dbHelper(leakage);
        ArrayList<myMessage> checkConversation = helper.getConversationMessages(currentNumber);
        helper.close();
        final int newChatIndex = checkConversation.size() - 1;
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


        final ArrayList<myMessage> newConversation = checkConversation;
        runOnUiThread(new Runnable() {
            public void run() {
                for (myMessage m : newConversation) {
                    chatAdapter.addItem(m.getBody(), m.getSent());
                }
            }
        });

        currentChatIndex = newChatIndex;
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
            //registerReceiver(onNewMsg, onNewMsgFilter);
//            registerReceiver(onNewMsgSend, new IntentFilter("onNewMsgSend"));
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            //unregisterReceiver(onNewMsg);
//            unregisterReceiver(onNewMsgSend);
            receiversRegistered = false;
        }

        //old code for preventing crash when pressing back button
        //try { unregisterReceiver(onNewMsg); } catch (IllegalArgumentException iae) { };
        //try { unregisterReceiver(onNewMsgSend); } catch (IllegalArgumentException iae) { };

        super.onPause();
    }

    protected void onStop() {
        System.out.println("Sender onStop");
        if (receiversRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            //unregisterReceiver(onNewMsg);
//            unregisterReceiver(onNewMsgSend);
            receiversRegistered = false;
        }
        super.onStop();
    }

    protected void onDestroy() {
        System.out.println("Sender onDestroy");
        if (receiversRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            //unregisterReceiver(onNewMsg);
//            unregisterReceiver(onNewMsgSend);
            receiversRegistered = false;
        }
        super.onDestroy();
    }



    private class chatAdapter extends BaseAdapter {
        private static final int SENT = 0;
        private static final int RECEIVED = 1;
        private static final int MAX_TYPES = RECEIVED + 1;

        private ArrayList<String> chatArray = new ArrayList<>();
        private LayoutInflater inflater;
        private ArrayList<Integer> sentArray = new ArrayList<>();

        public chatAdapter() {
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
