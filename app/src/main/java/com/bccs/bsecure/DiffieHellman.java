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
    //RFC 3526-4 http://tools.ietf.org/html/rfc3526#page-4
    //This prime is: 2^3072 - 2^3008 - 1 + 2^64 * { [2^2942 pi] + 1690314 }
    private final BigInteger p = new BigInteger("5809605995369958062791915965639201402176612226" +
            "9029005337029008827797361778909908614720947744773395811473734101856463783280437298" +
            "0075047009821092448786693505916437158816804754094398164451663275506750162643455639" +
            "8193186628990071248660819361205119793693985433297036118232914410171876807536457391" +
            "2778570118498974102075191053333558011211093568974594262718454713979526759594407934" +
            "9307162839412278051012461848823260246464987685045886124578424092925842628769970531" +
            "2584509625419513463605155428017165714465363094021609290561084025893662561222573202" +
            "0828657978218652709911450822006569781771928270245389902399691755461907706456858934" +
            "3801171443042640933867631474357115453714203157300427642870143303638180170530865983" +
            "0751190352946025482059931306571004727362479688415574702596946457770284148435989129" +
            "6328539183921179974726326930781131298864873993477969827727846158652326212896569442" +
            "84216824611318709764535152507354116344703769998514148343807");
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
