package com.bccs.bsecure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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

public class BluetoothPackage implements Serializable {

    private String[] keys;
    private int protocolCode;
    private int keyAmount;
    private int expireCount;

    /**
     * Constructor for ensuring options are the same.
     *
     * @param amount       The amount of keys the devices have agreed to display.
     * @param expireCount  The agreed upon count of message expiration.
     * @param protocolCode Stage of the exchange normally "Agreement" with this constructor
     */
    public BluetoothPackage(int amount, int expireCount, int protocolCode) {
        this.keyAmount = amount;
        this.expireCount = expireCount;
        this.protocolCode = protocolCode;
    }

    /**
     * Constructor for exchanging keys.
     *
     * @param keys         Encoded public keys to send.
     * @param protocolCode Stage of the exchange.
     */
    public BluetoothPackage(String[] keys, int protocolCode) {
        this.protocolCode = protocolCode;
        this.keys = keys;
    }

    public int getProtocolCode() {
        return protocolCode;
    }

    public String[] getKeys() {
        return keys;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(this);
        return b.toByteArray();
    }

    public int getKeyAmount() {
        return keyAmount;
    }

    public int getExpireCount() {
        return expireCount;
    }
    /**
     * De-serialize method
     *
     public static BluetoothPackage deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
     ByteArrayInputStream b = new ByteArrayInputStream(bytes);
     ObjectInputStream o = new ObjectInputStream(b);
     return (BluetoothPackage) o.readObject();
     }
     */
}
