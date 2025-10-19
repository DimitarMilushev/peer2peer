package main.java.d.milushev;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class BufferUtils
{
    public static String BufferToString(ByteBuffer buffer, int bytesRead)
    {
        return new String(buffer.array(), buffer.position(), bytesRead);
    }
}
