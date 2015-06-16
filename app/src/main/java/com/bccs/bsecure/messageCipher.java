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
 * the cipher. For now it truncates to the first 16 bytes of the key.
 * Edited encryption and decryption methods to convert the keys from hexadecimal to bytes rather than
 * taking the ASCII value bytes.
 *
 * Modified by lucas.burdell on 6/16/2015
 * Switched from hexadecimal encoding and decoding of bytes to base64.
 *
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
            byte[] iv1 = cutIV(fromBase64String(key2));
            IvParameterSpec iv = new IvParameterSpec(iv1);

            SecretKeySpec skeySpec = new SecretKeySpec(fromBase64String(key1),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes());

//            System.out.println("encrypted string:"
//                    + Base64.encodeToString(encrypted, 0)           );
            return toBase64String(encrypted);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //Dr. Coogan's decrypt method, some variations made

    public static String decrypt(String encrypted, String key1, String key2) {
        try {
            byte[] iv1 = cutIV(fromBase64String(key2));
            IvParameterSpec iv = new IvParameterSpec(iv1);

            SecretKeySpec skeySpec = new SecretKeySpec(fromBase64String(key1),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            //byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
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