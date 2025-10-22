package main.java.d.milushev.p2p.server;


import java.nio.ByteBuffer;


public class BufferUtils
{
    public static String BufferToString(ByteBuffer buffer, int bytesRead)
    {
        return new String(buffer.array(), buffer.position(), bytesRead);
    }
}
