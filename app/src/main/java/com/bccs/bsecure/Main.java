package com.bccs.bsecure;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 * <p/>
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 */

public class Main extends AppCompatActivity implements WipeActiveConversationsDialog.WipeActiveConversationsDialogListener {

    //Global class variables

    private contactsAdapter activeInfoAdapter;
    //The list view that contains the on-screen info being displayed
    private ListView userListView;
    //ArrayList storing names and numbers in outbox
    private ArrayList<Integer> activeNums = new ArrayList<>();
    private boolean receiverRegistered = false;

    private Button.OnClickListener optionsClick = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            WipeActiveConversationsDialog dialog = new WipeActiveConversationsDialog();
            dialog.show(getFragmentManager(), "active_conversations_options");

        }
    };

    private AdapterView.OnItemClickListener onListClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            Intent conversationIntent = new Intent(getApplicationContext(), Conversation.class);
            conversationIntent.putExtra("name", activeNums.get(pos));
            conversationIntent.putExtra("number", activeNums.get(pos));
            startActivity(conversationIntent);
        }
    };

    SmsBroadcastReceiverxxx onNewMsg = new SmsBroadcastReceiverxxx() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateActiveNums();
        }
    };

    IntentFilter onNewMsgFilter = new IntentFilter("com.bccs.bsecure.msgReceived");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Contact.setBaseContext(this.getApplicationContext());

//        Intent intent = new Intent(this, TestActivity.class);
//        startActivity(intent);
//        System.exit(0);
        //Get ActionBar Item
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//        ActionBar actionBar = getActionBar();



        //initialize global variables
        userListView = (ListView) findViewById(R.id.usersListView);
        //Define Listener method
        userListView.setOnItemClickListener(onListClick);
        //Read outbox for active conversation information
        //Initialize the adapter to hold the names of active contacts
        activeInfoAdapter = new contactsAdapter();
        //Display the names.
        userListView.setAdapter(activeInfoAdapter);
        updateActiveNums();

        findViewById(R.id.activeConversationOptionsButton).setOnClickListener(optionsClick);


        LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
        receiverRegistered = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
                onBackPressed();
                return true;
        }
    }

    public void openNewMessage(){
        Intent intent = new Intent(this, CreateMessage.class);
        startActivity(intent);
    }
    public void openContacts(){
        Intent intent = new Intent(this, Contacts.class);
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


    protected void onStart() {

        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiverRegistered = true;
        }
        updateActiveNums();
        super.onStart();
    }

    private void updateActiveNums() {

        ConversationManager manager = ConversationManager.getManager(this);

        ArrayList<Integer> activeConversations = manager.getActiveConversations();
        for (int s : activeConversations) {
            ConversationManager.ConversationHelper helper = ConversationManager.getConversation(s);
            MyMessagexxx message = helper.getLastMessage();
            if (!activeNums.contains(s)) {
                activeNums.add(s);
                activeInfoAdapter.addItem(message);
            } else {
                activeInfoAdapter.updateMessage(message);
            }
        }
    }

    @Override
    public void onOKPressed(DialogFragment dialog) {

        ConversationManager manager = ConversationManager.getManager(this);
        manager.clearAllConversations();
        dialog.dismiss();
        activeNums = new ArrayList<>();
        activeInfoAdapter.clearItems();
        updateActiveNums();
    }

    @Override
    public void onCancelPressed(DialogFragment dialog) {
        dialog.dismiss();
    }


    // Custom Adapter to display the differing layouts for chat.
    private class contactsAdapter extends BaseAdapter {

        private static final int MAX_TYPES = 1;

        private ArrayList<MyMessagexxx> contactsArray = new ArrayList<>();
        private LayoutInflater inflater;

        public contactsAdapter() {
            //create inflater that will hold the chat boxes
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(MyMessagexxx message) {
            contactsArray.add(message);
            notifyDataSetChanged();
        }

        public void clearItems() {
            contactsArray = new ArrayList<>();
            notifyDataSetChanged();
        }

        public void updateMessage(MyMessagexxx message) {

            for (MyMessagexxx m : contactsArray) {
                if (message.get_name().equals(m.get_name())) {


                    if (message.get_time() > m.get_time()) {

                        m.setBody(message.getBody());
                    } else {

                    }
                    break;
                }
            }
            notifyDataSetChanged();
        }


        public ArrayList<MyMessagexxx> getContactsArray() {
            ArrayList<MyMessagexxx> newChat = new ArrayList<>();
            for (MyMessagexxx m : contactsArray) {
                newChat.add(m);
            }
            return newChat;
        }


        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return MAX_TYPES;
        }

        @Override
        public int getCount() {
            return contactsArray.size();
        }

        @Override
        public MyMessagexxx getItem(int position) {
            return contactsArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout holder = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    default:
                        convertView = inflater.inflate(R.layout.new_row_layout, null);
                        holder = (RelativeLayout) convertView;
                        break;
                }
            } else {
                //holder.setOnClickListener();
                holder = (RelativeLayout) convertView;
            }
            final MyMessagexxx item = getItem(position);
            TextView contactName = (TextView) holder.getChildAt(1);
            TextView lastMessage = (TextView) holder.getChildAt(2);
            contactName.setText(item.get_name());
            lastMessage.setText((item.getSent() ? "You: " : item.get_name() + ": ") + item.getBody());
            holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent conversationIntent = new Intent(getApplicationContext(), Conversation.class);
                    conversationIntent.putExtra("contactid", item.getId());
                    startActivity(conversationIntent);
                }
            });
            return convertView;
        }
    }

    protected void onResume() {

        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNewMsg, onNewMsgFilter);
            receiverRegistered = true;
        }
        updateActiveNums();
        super.onResume();
    }

    protected void onPause() {

        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
        super.onPause();
    }

    protected void onStop() {

        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
        super.onStop();
    }


    // onDestroy is called when the entire application is exited
    // We can close our DB from here.
    protected void onDestroy() {

        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onNewMsg);
            receiverRegistered = false;
        }
        ConversationManager.closeConnection();
        super.onDestroy();
    }
}
