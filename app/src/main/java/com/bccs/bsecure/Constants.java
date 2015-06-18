package com.bccs.bsecure;

/**
 * Created by shane.nalezyty on 6/16/2015.
 */
public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    //Codes for use in our bluetooth diffiehellman exchange protocol
    int EXCHANGE_FIRST_TRADE = 1;
    int EXCHANGE_SECOND_TRADE = 2;
    int EXCHANGE_FINALIZATION_FIRST_ACK = 3;
    int EXCHANGE_FINALIZATION_SECOND_ACK = 4;
    int EXCHANGE_ERROR = 5;

    //Amount of keys to generate
    int KEY_AMOUNT = 100;
}
