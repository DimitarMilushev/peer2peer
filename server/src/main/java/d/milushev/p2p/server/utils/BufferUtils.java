package main.java.d.milushev.p2p.server.utils;


import java.nio.ByteBuffer;


public class BufferUtils
{
    public static String bufferToString(ByteBuffer buffer, int bytesRead)
    {
        return new String(buffer.array(), buffer.position(), bytesRead);
    }
}
