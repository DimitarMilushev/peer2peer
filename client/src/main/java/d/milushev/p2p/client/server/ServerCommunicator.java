package main.java.d.milushev.p2p.client.server;


import d.milushev.p2p.network_utils.SocketUtils;
import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Establishes connections to the underlying server. Handles Socket lifecycle and exposes IO interface.
 */
public class ServerCommunicator
{
    private final static Logger LOG = LogManager.getLogger(ServerCommunicator.class.getName());

    private final static int DEFAULT_BUFFER_SIZE = 1024;

    private final InetSocketAddress targetAddress;
    private final AtomicBoolean running;
    private final AtomicBoolean awaitingResponse;
    private final Queue<String> commandsQueue;

    private final ExecutorService executor;


    public ServerCommunicator(String host, int port)
    {
        this.targetAddress = new InetSocketAddress(host, port);
        this.running = new AtomicBoolean(false);
        this.commandsQueue = new LinkedList<>();
        this.executor = Executors.newSingleThreadExecutor();
        awaitingResponse = new AtomicBoolean(false);
    }


    public void start()
                    throws IOException
    {
        executor.submit(() -> {
            try
            {
                this.listen();
            } catch (IOException e)
            {
                LOG.warn("Error starting server communicator", e);
            }
            finally
            {
                executor.shutdown();
            }
        });
    }


    private void listen()
                    throws IOException
    {
        try (Selector selector = Selector.open();
             SocketChannel clientChannel = SocketChannel.open())
        {
            clientChannel.configureBlocking(false);
            clientChannel.connect(targetAddress);
            clientChannel.register(selector, SelectionKey.OP_CONNECT);

            final ByteBuffer writeBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            final ByteBuffer readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);

            running.set(true);
            LOG.info("Client started. Type messages to send:");

            while (running.get())
            {
                try
                {
                    if (selector.select(1000) == 0)
                    {
                        continue;
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                for (var key : selector.selectedKeys())
                {
                    if (key.isWritable())
                    {
                        handleWrite(key, writeBuffer);
                    }
                    else if (key.isReadable())
                    {
                       handleRead(key, clientChannel, readBuffer);
                    }
                    else if (key.isConnectable())
                    {
                        while (!clientChannel.finishConnect())
                        {
                            System.out.println("Connecting...");
                        }
                        System.out.println("Connected");
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                selector.selectedKeys().clear();
            }
        }
        finally

        {
            running.set(false);
            executor.shutdown();
        }

    }


    private void handleRead(SelectionKey key, SocketChannel clientChannel, ByteBuffer readBuffer)
                    throws IOException
    {
        final String message = SocketUtils.readFromChannel(clientChannel, readBuffer);
        LOG.info("Received: {}", message);
        awaitingResponse.set(false);
        key.interestOps(SelectionKey.OP_WRITE);
    }


    private void handleWrite(SelectionKey key, ByteBuffer writeBuffer)
                    throws IOException
    {
        if (commandsQueue.isEmpty())
        {
            return;
        }

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
            awaitingResponse.set(true);

            LOG.debug("Successfully handled WRITE for [{}]: {}", socket.getRemoteSocketAddress(), message);
        }
    }


    public void send(String message)
    {
        commandsQueue.add(message);
    }
}
