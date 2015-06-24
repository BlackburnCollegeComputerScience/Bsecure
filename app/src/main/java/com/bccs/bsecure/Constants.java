package com.bccs.bsecure;

/**
 *
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 *
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_TOAST = 3;

    //Codes for use in our bluetooth diffie hellman exchange protocol
    int EXCHANGE_FIRST_TRADE = 1;
    int EXCHANGE_SECOND_TRADE = 2;
    int EXCHANGE_FINALIZATION = 3;
    int EXCHANGE_AGREEMENT = 4;
    int EXCHANGE_AGREEMENT_ALLOW = 5;
    int EXCHANGE_AGREEMENT_DENY = 6;

    //Request codes
    int REQUEST_ENABLE_BT = 1;
    int REQUEST_ENABLE_BT_DIS = 2;
    int REQUEST_KEYS = 3;

    //Amount of keys to exchange
    int KEY_AMOUNT = 100;

}
