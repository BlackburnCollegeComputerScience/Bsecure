package com.bccs.bsecure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/*
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

/**
 * Bluetooth package for easily serializing varying data.
 */
public class BluetoothPackage implements Serializable {

    /**
     * Public key encodings
     */
    private String[] keys;
    private String[] ivs;
    /**
     * Code to inform the receiver how to handle the package.
     */
    private int protocolCode;
    /**
     * Used in the agreement stage of the protocol. Minimum number of messages that
     * a key should be used on.
     */
    private int minExpire;
    /**
     * Used in the agreement stage of the protocol. Maximum number of messages that
     * a key should be used on.
     */
    private int maxExpire;

    /**
     * Constructor for just exchanging agreement codes.
     * @param protocolCode Agreement or Denial of security parameters.
     */
    public BluetoothPackage(int protocolCode) {
        this.protocolCode = protocolCode;
    }

    /**
     * Constructor for exchanging security parameters
     * @param minExpire Minimum number of messages that a key should be used on.
     * @param maxExpire Maximum number of messages that a key should be used on.
     * @param protocolCode Stage of the exchange
     */
    public BluetoothPackage(int minExpire, int maxExpire, int protocolCode) {
        this.minExpire = minExpire;
        this.maxExpire = maxExpire;
        this.protocolCode = protocolCode;
    }

    public BluetoothPackage(String[] keys, String[] ivs, int protocolCode) {
        this.protocolCode = protocolCode;
        this.ivs = ivs;
        this.keys = keys;
    }

    /**
     * Constructor for exchanging keys.
     * @param keys Encoded public keys to send.
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

    public String[] getIvs() {
        return ivs;
    }

    public int getMinExpire() {
        return minExpire;
    }

    public int getMaxExpire() {
        return maxExpire;
    }

    /**
     * Serializes this package to send over the bluetooth byte stream.
     * @return Array of bytes representing this package
     * @throws IOException
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(this);
        return b.toByteArray();
    }
    /*
     * De-serialize method
     *
     public static BluetoothPackage deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
     ByteArrayInputStream b = new ByteArrayInputStream(bytes);
     ObjectInputStream o = new ObjectInputStream(b);
     return (BluetoothPackage) o.readObject();
     }
     */
}
