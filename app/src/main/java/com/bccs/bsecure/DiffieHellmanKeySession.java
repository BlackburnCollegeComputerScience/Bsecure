package com.bccs.bsecure;

import android.util.Base64;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

/**
 * Created by shane.nalezyty on 6/5/2015.
 */
public class DiffieHellmanKeySession {

    /*

    Example usage:

        //Alice
        DiffieHellmanKeySession alice = new DiffieHellmanKeySession(); //generate keypair
        String alicePub = alice.packKey(); //returns alice's public key in base64

        //Send to Bob
        DiffieHellmanKeySession bob = new DiffieHellmanKeySession(alicePub); //passing alicePub as constructor to get public params alice used
        String bobPub = bob.packKey(); //returns bob's public key in base64
        String bobSecret = bob.packSecret(alicePub); //returns private shared key in base64

        //Send to Alice
        String aliceSecret = alice.packSecret(bobPub); //returns private shared key in base64


     */



    //Large Prime P
    //RFC 3526-3 http://tools.ietf.org/html/rfc3526#page-4
    //This prime is: 2^2048 - 2^1984 - 1 + 2^64 * { [2^1918 pi] + 124476 }
    private final BigInteger p = new BigInteger("32317006071311007300338913926423828248817941241" +
            "14023911284200975140074170663435422261968941736356934711790173790970419175460587320" +
            "91950288537589861856221532121754125149017745202702357960782362488842461894775876411" +
            "05928646099411723245426622522193230540919037680524235519125679715870117001058055877" +
            "65103886184728025797605490356973256152616708133936179954133647655916036831789672907" +
            "31783845896806396719009772021941686472258710314113364293195361934716365332097170774" +
            "48227988588565369208645296636077250268955505928362751121174096972998068410554359584" +
            "866583291642136218231078990999448652468262416972035911852507045361090559");
    //Generator g
    private final BigInteger g = BigInteger.valueOf(2);

    private KeyAgreement keyAgreement;
    private KeyPair pair;
    private SecretKey secret;
    private String hashOfSecret;


    public DiffieHellmanKeySession() throws Exception {
        DHParameterSpec dhGenParameterSpec = new DHParameterSpec(p, g);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhGenParameterSpec);
        this.pair = keyPairGenerator.genKeyPair();
        this.keyAgreement = KeyAgreement.getInstance("DH");
        this.keyAgreement.init(pair.getPrivate());
    }

    public DiffieHellmanKeySession(byte[] encodedKey) throws Exception {
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(encodedKey);
        DHPublicKey alicePubKey = (DHPublicKey) keyFac.generatePublic(x509KeySpec);
        DHParameterSpec dhParamSpec = alicePubKey.getParams();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhParamSpec);

        this.pair = keyPairGenerator.generateKeyPair();
        this.keyAgreement = KeyAgreement.getInstance("DH");
        this.keyAgreement.init(pair.getPrivate());
    }

    public DiffieHellmanKeySession(String key) throws Exception {
        //this(hexStringToByteArray(key));
        this(fromBase64String(key));
    }

    public PublicKey getPublicKey() {
        return pair.getPublic();
    }

    public String packKey() {
        //return toHexString(this.getPublicKey().getEncoded());
        return toBase64String(this.getPublicKey().getEncoded());
    }

    public String packKey(byte[] encodedKey) {
        return toBase64String(encodedKey);
    }

    public byte[] unpackKey(String key) {
        return fromBase64String(key);
    }

    public SecretKey generateSecret(byte[] otherPublicKey) throws Exception {
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPublicKey);
        PublicKey newPubKey = keyFac.generatePublic(x509KeySpec);
        this.keyAgreement.doPhase(newPubKey, true);
        return this.keyAgreement.generateSecret("AES");
    }

    public SecretKey generateSecret(String otherPublicKey) throws Exception {
        secret = this.generateSecret(unpackKey(otherPublicKey));
        return secret;
    }

    public String hashSecret() throws Exception {
        if (hashOfSecret == null) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] DHOut = secret.getEncoded();
            md.update(DHOut);
            byte[] digest = md.digest();
            hashOfSecret = toBase64String(digest);
        }
        return hashOfSecret;
    }
    public String packSecret(String otherPublicKey) throws Exception {
        this.generateSecret(otherPublicKey);
        byte[] DHOut = secret.getEncoded();
        return toBase64String(DHOut);
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

    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    public static byte[] getHash(byte[] block) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(block);
        return md.digest();
    }

    public static byte[] fromBase64String(String s) {
        return Base64.decode(s, 0);
    }
    public static String toBase64String(byte[] block) {
        return Base64.encodeToString(block, 0); //This is for Android's version
    }


    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();

        int len = block.length;

        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
        }
        return buf.toString();
    }



}
