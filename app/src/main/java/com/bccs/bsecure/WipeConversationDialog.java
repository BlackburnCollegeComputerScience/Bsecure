package com.bccs.bsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by lucas.burdell on 6/17/2015.
 */
public class WipeConversationDialog extends DialogFragment {


    public interface WipeConversationDialogListener {
        public void onOKPressed(DialogFragment dialog);
        public void onCancelPressed(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    WipeConversationDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the Listener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (WipeConversationDialogListener) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Erase conversation?")
                .setMessage("You will lose all of your current conversation! Are you sure you want to erase" +
                        " this conversation?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onOKPressed(WipeConversationDialog.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onCancelPressed(WipeConversationDialog.this);
                    }
                });

        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();

        return dialog;
    }



    private final class holder {
        private int b;
        public holder(int b) {
            this.b = b;
        }
        public int getValue() {
            return this.b;
        }
        public void setValue(int b) {
            this.b = b;
        }
    }
}
