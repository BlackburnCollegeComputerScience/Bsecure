package com.bccs.bsecure;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

/**
 * Created by shane.nalezyty on 6/5/2015.
 */
public class DiffieHellman {
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
}
