package com.bccs.bsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

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

public class WipeConversationDialog extends DialogFragment {


    interface WipeConversationDialogListener {
        void onOKPressed(DialogFragment dialog);
        void onCancelPressed(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    WipeConversationDialogListener listener;

    // Override the Fragment.onAttach() method to instantiate the Listener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (WipeConversationDialogListener) activity;
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
                        listener.onOKPressed(WipeConversationDialog.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onCancelPressed(WipeConversationDialog.this);
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