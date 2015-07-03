package com.bccs.bsecure;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 * <p>
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by lucas.burdell on 6/4/2015.
 * Seperate entity for handling the encryption and decryption of messages given the string and keys.
 * Current encryption methods were taken from the old messageSender and smsBroadcastReceiver files
 * written by traci.kamp.
 * <p>
 * Modified by lucas.burdell on 6/12/2015
 * Added code to shrink our initialization vector down to 16 bytes (128 bits) to be used with
 * the cipher. For now it truncates to the first 16 bytes of the key.
 * Edited encryption and decryption methods to convert the keys from hexadecimal to bytes rather than
 * taking the ASCII value bytes.
 * <p>
 * Modified by lucas.burdell on 6/16/2015
 * Switched from hexadecimal encoding and decoding of bytes to base64.
 */
public class MessageCipher {

    //Dr. Coogan's encrypt method, some variations made

    public static String encrypt(String value, String key1, String key2) {

        try {
            byte[] iv1 = fromBase64String(key2);
            IvParameterSpec iv = new IvParameterSpec(iv1);

            SecretKeySpec skeySpec = new SecretKeySpec(fromBase64String(key1),
                    "AES");
            System.out.println(fromBase64String(key1).length + " " + iv1.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return toBase64String(encrypted);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //Dr. Coogan's decrypt method, some variations made

    public static String decrypt(String encrypted, String key1, String key2) {
        try {
            byte[] iv1 = fromBase64String(key2);
            IvParameterSpec iv = new IvParameterSpec(iv1);

            SecretKeySpec skeySpec = new SecretKeySpec(fromBase64String(key1),
                    "AES");
            System.out.println(fromBase64String(key1).length + " " + iv1.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(fromBase64String(encrypted));


            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] fromBase64String(String s) {
        return Base64.decode(s, 0);
    }

    public static String toBase64String(byte[] block) {
        return Base64.encodeToString(block, 0);
    }

}