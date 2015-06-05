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
 */
public class messageCipher {


    //Dr. Coogan's encrypt method, some variations made

    public static String encrypt(String value, String key1, String key2) {

        try {
            IvParameterSpec iv = new IvParameterSpec(key2.getBytes("UTF-8"));

            SecretKeySpec skeySpec = new SecretKeySpec(key1.getBytes("UTF-8"),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes());

//            System.out.println("encrypted string:"
//                    + Base64.encodeToString(encrypted, 0));
            return Base64.encodeToString(encrypted, 0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //Dr. Coogan's decrypt method, some variations made
    //not in running state but need Dr. Coogan's expertise on Base64

    public static String decrypt(String encrypted, String key1, String key2) {
        try {
            IvParameterSpec iv = new IvParameterSpec(key2.getBytes("UTF-8"));

            SecretKeySpec skeySpec = new SecretKeySpec(key1.getBytes("UTF-8"),
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
}
