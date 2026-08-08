package d.milushev.p2p.network_utils;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class SocketUtils
{
    private SocketUtils()
    {
    }


    public static String readFromChannel(SocketChannel channel, ByteBuffer buffer)
                    throws IOException
    {
        Objects.requireNonNull(channel, "SocketChannel cannot be null");
        Objects.requireNonNull(buffer, "ByteBuffer cannot be null");

        buffer.clear();

        int bytesRead = channel.read(buffer);
        if (bytesRead == -1)
        {
            throw new IOException("End of stream reached while reading from channel");
        }

        final StringBuilder sb = new StringBuilder();
        while (bytesRead > 0)
        {
            buffer.flip();
            sb.append(BufferUtils.bufferToString(buffer, bytesRead));

            buffer.clear();
            bytesRead = channel.read(buffer);
        }

        return sb.toString();
    }


    public static void writeToChannel(SocketChannel channel, ByteBuffer buffer, String message)
                    throws IOException
    {
        Objects.requireNonNull(channel, "SocketChannel cannot be null");
        Objects.requireNonNull(buffer, "ByteBuffer cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");

        buffer.clear();

        //TODO: Ensure this buffer handles large messages
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }

        buffer.clear();
    }
}
