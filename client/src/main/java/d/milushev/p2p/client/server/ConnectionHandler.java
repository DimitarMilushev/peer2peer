package main.java.d.milushev.p2p.client.server;


import d.milushev.p2p.network_utils.SocketUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConnectionHandler
{
    private static final Logger LOG = LogManager.getLogger(ConnectionHandler.class);
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private final Queue<String> commandsQueue;
    private CompletableFuture<String> response;

    private final ByteBuffer writeBuffer;
    private final ByteBuffer readBuffer;


    public ConnectionHandler()
    {
        commandsQueue = new LinkedList<>();
        response = null;

        writeBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }


    public void handleWrite(SelectionKey key)
                    throws Exception
    {
        if (commandsQueue.isEmpty())
        {
            return;
        }

        LOG.info("Handling WRITE for channel: {}", key.channel());
        writeBuffer.clear();

        final String message = commandsQueue.poll();
        if (message == null || message.isBlank())
        {
            LOG.warn("Empty command. Ignoring...");
            return;
        }

        if (key.channel() instanceof SocketChannel server)
        {
            final Socket socket = server.socket();

            LOG.info("Handling WRITE for [{}]", socket.getRemoteSocketAddress());

            SocketUtils.writeToChannel(server, writeBuffer, message);

            key.interestOps(SelectionKey.OP_READ);
            response = new CompletableFuture<>();

            LOG.debug("Successfully handled WRITE for [{}]: {}", socket.getRemoteSocketAddress(), message);
        }
    }


    public void handleRead(SelectionKey key)
                    throws Exception
    {
        LOG.info("Handling READ for channel: {}", key.channel());

        if (key.channel() instanceof SocketChannel clientChannel)
        {
            final String message = SocketUtils.readFromChannel(clientChannel, readBuffer);
            LOG.info("Received: {}", message);

            response.complete(message);
            key.interestOps(SelectionKey.OP_WRITE);

            return;
        }

        throw new Exception("Invalid channel was opened for READ operation");
    }


    public void handleConnect(SelectionKey key)
                    throws Exception
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            while (!clientChannel.finishConnect())
            {
                LOG.info("Connecting...");
            }

            LOG.info("Connected to server: {}", clientChannel.getRemoteAddress());
            key.interestOps(SelectionKey.OP_WRITE);
            return;
        }

        throw new Exception("Invalid channel was opened for CONNECT operation");
    }


    public void enqueueMessage(String message)
                    throws Exception
    {
        if (!commandsQueue.add(message))
        {
            throw new Exception("Failed to enqueue message: " + message);
        }

        LOG.info("Successfully enqueued message: {}", message);
    }


    public String getResponse()
    {
        if (response == null)
        {
            return null;
        }

        try
        {
            return response.get();
        }
        catch (Exception e)
        {
            LOG.error("Error getting response", e);
            return null;
        }
    }
}
