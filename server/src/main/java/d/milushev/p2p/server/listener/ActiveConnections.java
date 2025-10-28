package main.java.d.milushev.p2p.server.listener;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class ActiveConnections
{
    private static final int DEFAULT_BUFFER_SIZE_BYTES = 1024;

    private final Map<SocketChannel, ByteBuffer> connections;


    public ActiveConnections()
    {
        this.connections = new HashMap<>();
    }


    public ByteBuffer getBuffer(SocketChannel channel) throws Exception
    {
        if (!this.connections.containsKey(channel))
        {
            throw new Exception("No such connection [" + channel.getRemoteAddress() + "]");
        }

        return this.connections.get(channel);
    }


    public void add(SocketChannel channel) throws Exception
    {
        if (this.connections.containsKey(channel))
        {
            throw new Exception("Connection already exists [" + channel.getRemoteAddress() + "]");
        }

        this.connections.put(channel, ByteBuffer.allocate(DEFAULT_BUFFER_SIZE_BYTES));
    }


    public void remove(SocketChannel channel) throws Exception
    {
        if (!this.connections.containsKey(channel))
        {
            throw new Exception("No such connection [" + channel.getRemoteAddress() + "]");
        }

        connections.remove(channel);
    }


    public void closeAll() throws IOException
    {
        for (var c : connections.keySet())
        {
            c.close();
            connections.remove(c);
        }
    }


    @Override
    public String toString()
    {
        return connections.keySet().stream().map(this::getRemoteAddress).collect(Collectors.joining());
    }


    private String getRemoteAddress(SocketChannel channel)
    {
        try
        {
            return channel.getRemoteAddress().toString();
        }
        catch (IOException e)
        {
            System.out.println("Failed to retrieve address " + e.getMessage());

            return "";
        }
    }
}
