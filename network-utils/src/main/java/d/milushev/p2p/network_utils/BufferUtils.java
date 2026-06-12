package d.milushev.p2p.network_utils;


import java.nio.ByteBuffer;
import java.util.Objects;


public class BufferUtils
{
    private BufferUtils()
    {
    }


    public static String bufferToString(ByteBuffer buffer, int bytesRead)
    {
        Objects.requireNonNull(buffer, "Buffer cannot be null");
        return new String(buffer.array(), buffer.position(), bytesRead);
    }
}
