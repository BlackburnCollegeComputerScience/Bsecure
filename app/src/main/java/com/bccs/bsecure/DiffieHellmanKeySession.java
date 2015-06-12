package com.bccs.bsecure;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
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

    private final int l = 56;

    private KeyAgreement keyAgreement;
    private KeyPair pair;


    public DiffieHellmanKeySession() throws Exception {
        DHParameterSpec dhGenParameterSpec = new DHParameterSpec(p, g, l);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhGenParameterSpec);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        this.pair = keyPairGenerator.genKeyPair();
        this.keyAgreement = KeyAgreement.getInstance("DH");
        this.keyAgreement.init(pair.getPrivate());
    }

    // Most likely not safe but let's try it
    public static byte[] truncateKey(byte[] key) {
        //get 128 bit key
        byte[] truncate = new byte[128];
        for (int i = 0; i < 128; i++) {
            truncate[i] = key[i];
        }
        return truncate;
    }

    public DiffieHellmanKeySession(byte[] encodedKey) throws Exception {
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(encodedKey);
        PublicKey alicePubKey = keyFac.generatePublic(x509KeySpec);
        DHParameterSpec dhParamSpec = ((DHPublicKey)alicePubKey).getParams();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhParamSpec);

        this.pair = keyPairGenerator.generateKeyPair();
        this.keyAgreement = KeyAgreement.getInstance("DH");
        this.keyAgreement.init(pair.getPrivate());
    }

    public DiffieHellmanKeySession(String key) throws Exception {
        this(hexStringToByteArray(key));
        //this(Base64.decode(key, 0));
    }

    public PublicKey getPublicKey() {
        return pair.getPublic();
    }

    public String packKey() {
        return toHexString(this.getPublicKey().getEncoded());
        //return Base64.encodeToString(this.getPublicKey().getEncoded(), 0);
    }

    public String packKey(byte[] encodedKey) {
        return toHexString(encodedKey);
        //return Base64.getEncoder().encodeToString(encodedKey);
    }

    public byte[] unpackKey(String key) {
        return hexStringToByteArray(key);
    }

    public SecretKey generateSecret(byte[] otherPublicKey) throws Exception {
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPublicKey);
        PublicKey newPubKey = keyFac.generatePublic(x509KeySpec);
        this.keyAgreement.doPhase(newPubKey, true);
        return this.keyAgreement.generateSecret("AES");
    }

    public SecretKey generateSecret(String otherPublicKey) throws Exception {
        return this.generateSecret(unpackKey(otherPublicKey));
    }

    public String packSecret(String otherPublicKey) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] DHOut = this.generateSecret(otherPublicKey).getEncoded();
        String hexDHOut = toHexString(DHOut);
        System.out.println("Old key: ");
        System.out.println(hexDHOut);
        System.out.println("Of length: " + hexDHOut.length());
        System.out.println("With byte size " + DHOut.length);
        md.update(DHOut);
        byte[] digest = md.digest();
        String secretKey = toHexString(digest);
        System.out.println("New key: ");
        System.out.println(secretKey);
        System.out.println("Of length: " + secretKey.length());
        System.out.println("With byte size " + digest.length);
        return secretKey;
    }



    /*

    public KeyPair getKeyPair() {
        KeyPair newPair = null;
        DHParameterSpec dhGenParameterSpec = new DHParameterSpec(p, g);
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(dhGenParameterSpec);

            newPair = keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidAlgorithmParameterException e) {
        }
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(newPair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return newPair;
    }

    */


    /*

    public KeyPair getKeyPair() {
        KeyPair newPair = null;
        DHParameterSpec dhGenParameterSpec = new DHParameterSpec(p, g);
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(dhGenParameterSpec);

            newPair = keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidAlgorithmParameterException e) {
        }
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(newPair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return newPair;
    }

    */


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


    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();

        int len = block.length;

        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
        }
        return buf.toString();
    }



}
