package com.bccs.bsecure;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by lucas.burdell on 6/4/2015.
 * Seperate entity for handling the encryption and decryption of messages given the string and keys.
 * Current encryption methods were taken from the old messageSender and smsBroadcastReceiver files
 * written by traci.kamp.
 *
 * Modified by lucas.burdell on 6/12/2015
 * Added code to shrink our initialization vector down to 16 bytes (128 bits) to be used with
 * the cipher. For now it truncates to the first 16 bits of the key.
 * Edited encryption and decryption methods to convert the keys from hexadecimal to bytes rather than
 * taking the ASCII value bytes.
 */
public class messageCipher {


    public static byte[] cutIV(byte[] IV) {
        int len = 16;
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            b[i] = IV[i];
        }
        return b;
    }

    //Dr. Coogan's encrypt method, some variations made

    public static String encrypt(String value, String key1, String key2) {

        try {
            byte[] iv1 = cutIV(hexStringToByteArray(key2));
            IvParameterSpec iv = new IvParameterSpec(iv1);

            SecretKeySpec skeySpec = new SecretKeySpec(hexStringToByteArray(key1),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes());

//            System.out.println("encrypted string:"
//                    + Base64.encodeToString(encrypted, 0)           );
            return Base64.encodeToString(encrypted, 0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //Dr. Coogan's decrypt method, some variations made

    public static String decrypt(String encrypted, String key1, String key2) {
        try {
            byte[] iv1 = cutIV(hexStringToByteArray(key2));
            IvParameterSpec iv = new IvParameterSpec(iv1);

            SecretKeySpec skeySpec = new SecretKeySpec(hexStringToByteArray(key1),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            //byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
            byte[] original = cipher.doFinal(Base64.decode(encrypted, 0));


            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
