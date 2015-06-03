package com.bccs.bsecure;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

/*

 * Created by traci.kamp on 10/7/2014.
    This is the ListUsers class, which will display a list of all contacts
    stored in the phone to the user. They will be selectable for messaging.

 * Modified by traci.kamp on 11/10/2014 -> name changed to listContacts
    Should be modified to only list of users with an active conversation
    via a Database of some kind. Need to do research/investigation/checking
    on this to see if Android uses an accessible default mechanism for this
    already.

    Added dpHelper.

 * Modified by traci.kamp on 11/13/2014.
    Removed several unnecessary methods (setConversations() and getContacts()) as my new
    code snippets at the end of the onCreate() method now take care of all that. Receiving
    an error when clicking names - need to resolve.

    UPDATE: Error resolved by changing the array list being accessed by the Intent
    in the onItemClickListener method.

 * Update written by traci.kamp on 11/28/2014.
    Populated outbox database for testing use. On application run, I received the
    following error:
    "I/Choreographer? Skipped 40 frames!  The application may be doing too much work on its main thread."
    Initial StackOverflow research suggests that my use of ArrayLists is
    inefficient and that the drawing of certain objects in the application is
    taking too long. Threading will need to be explored or a more efficient
    use of ArrayLists is required.
    TODO: Research threading, ArrayLists, best practices, and ArrayList efficiency in Android

 * Update written by traci.kamp on 12/1/2014.
    Removed some print statements from dbHelper class (debugging souts) and
    the above error stopped appearing. Printing must have been slowing the
    rate at which the frames completed.
    Need to implement a "New conversation" button that allows a user to
    enter a phone number or to traverse all phone contacts and select a
    contact to begin a conversation with.
    TODO: Create a way to start a new conversation - will need to traverse all contacts.

 */


public class listContacts extends Activity {
    //Global class variables

    //The SQLite DB helper - traverses outbox
    public dbHelper appHelper;
    private boolean dbActive = false;
    //The adapter to use when displaying to the screen.
    private ArrayAdapter<String> activeInfoAdapter;
    //The list view that contains the on-screen info being displayed
    private ListView userListView;
    //ArrayList storing names and numbers in outbox
    private ArrayList<String>[] activeNums;
    private Button newConvButton;
    /*
        This method handles clicks on names displayed to the user.
        It passes an intent to the history/messaging view with the
        appropriate name and phone number of the clicked user.
     */
    private AdapterView.OnItemClickListener onListClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            Intent conversationIntent = new Intent(getApplicationContext(), messageSender.class);
            conversationIntent.putExtra("name", activeNums[1].get(pos));
            conversationIntent.putExtra("number", activeNums[0].get(pos));
            startActivity(conversationIntent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_contacts);
        setTitle("Conversations");
        appHelper = new dbHelper(this);
        dbActive = true;
        newConvButton = (Button) findViewById(R.id.New);
        //initialize global variables
        userListView = (ListView) findViewById(R.id.usersListView);
        //Define Listener method
        userListView.setOnItemClickListener(onListClick);
        //Read outbox for active conversation information
        activeNums = appHelper.getActiveNumbers();
        //Initialize the adapter to hold the names of active contacts
        activeInfoAdapter = new ArrayAdapter<String>
                (getApplicationContext(), R.layout.row_layout, activeNums[1]);
        //Display the names.
        userListView.setAdapter(activeInfoAdapter);
        newConvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent cI = new Intent(getApplicationContext(), messageSender.class);
                startActivity(cI);
            }
        });
    }

    protected void onStart() {
        System.out.println("Convo view onStart");
        if (!dbActive) {
            appHelper = new dbHelper(this);
            dbActive = true;
        }
        super.onStart();
    }

    protected void onResume() {
        System.out.println("Convo view onResume");
        if (!dbActive) {
            appHelper = new dbHelper(this);
            dbActive = true;
        }
        super.onResume();
    }

    protected void onPause() {
        System.out.println("Convo view onPause");
        appHelper.close();
        dbActive = false;
        super.onPause();
    }

    protected void onStop() {
        System.out.println("Convo view onStop");
        appHelper.close();
        dbActive = false;
        super.onStop();
    }

    protected void onDestroy() {
        System.out.println("Convo view onDestroy");
        super.onDestroy();
    }
}
