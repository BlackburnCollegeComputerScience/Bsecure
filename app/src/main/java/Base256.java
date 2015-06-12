import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * @author http://www.javacodegeeks.com/2010/11/java-best-practices-char-to-byte-and.html
 * @author Chronove
 * @version 1.0
 */

public class Base256 {

    public static String encode(byte[] bytes) {
        CharBuffer cBuffer = ByteBuffer.wrap(bytes).asCharBuffer();
        return cBuffer.toString();
    }

    public static byte[] decode(String str) {
        char[] buffer = str.toCharArray();
        byte[] b = new byte[buffer.length << 1];
        CharBuffer cBuffer = ByteBuffer.wrap(b).asCharBuffer();
        for(int i = 0; i < buffer.length; i++)
            cBuffer.put(buffer[i]);
        return b;
    }

}