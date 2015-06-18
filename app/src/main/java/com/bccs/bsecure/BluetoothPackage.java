package com.bccs.bsecure;

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

    private String[] keys = new String[Constants.KEY_AMOUNT];
    private int protocolCode;

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
}
