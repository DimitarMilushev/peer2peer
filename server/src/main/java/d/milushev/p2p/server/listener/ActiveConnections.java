package main.java.d.milushev.p2p.server.listener;


import main.java.d.milushev.p2p.server.exceptions.listener.ConnectionAlreadyExistsException;
import main.java.d.milushev.p2p.server.exceptions.listener.ConnectionNotFoundException;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class ActiveConnections
{
    private static final int DEFAULT_BUFFER_SIZE_BYTES = 1024;
    private static final String NULL_CHANNEL_MESSAGE = "Channel cannot be null";

    private final Map<Socket, ByteBuffer> connections;


    public ActiveConnections()
    {
        this.connections = new HashMap<>();
    }


    public ByteBuffer getBuffer(Socket channel) throws ConnectionNotFoundException
    {
        Objects.requireNonNull(channel, NULL_CHANNEL_MESSAGE);
        if (!this.connections.containsKey(channel))
        {
            throw new ConnectionNotFoundException("No such connection [" + channel.getRemoteSocketAddress() + "]");
        }

        return this.connections.get(channel);
    }


    public void add(Socket channel) throws ConnectionAlreadyExistsException
    {
        Objects.requireNonNull(channel, NULL_CHANNEL_MESSAGE);
        if (this.connections.containsKey(channel))
        {
            throw new ConnectionAlreadyExistsException("Connection already exists [" + channel.getRemoteSocketAddress() + "]");
        }

        this.connections.put(channel, ByteBuffer.allocate(DEFAULT_BUFFER_SIZE_BYTES));
    }


    public void remove(Socket channel) throws ConnectionNotFoundException
    {
        Objects.requireNonNull(channel, NULL_CHANNEL_MESSAGE);
        if (!this.connections.containsKey(channel))
        {
            throw new ConnectionNotFoundException("No such connection [" + channel.getRemoteSocketAddress() + "]");
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
        return connections.keySet().stream().map(x -> x.getRemoteSocketAddress().toString()).collect(Collectors.joining());
    }
}
