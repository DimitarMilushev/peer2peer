package main.java.d.milushev.p2p.client.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Establishes connections to the underlying server. Handles Socket lifecycle and exposes IO interface.
 */
public class ServerCommunicator
{
    private static final Logger LOG = LogManager.getLogger(ServerCommunicator.class.getName());

    private final InetSocketAddress targetAddress;
    private final AtomicBoolean running;

    private final ExecutorService executor;
    private final ConnectionHandler connectionHandler;
    private SocketChannel channel;


    public ServerCommunicator(String host, int port, ActiveUsersRepository repository)
    {
        this.targetAddress = new InetSocketAddress(host, port);
        this.running = new AtomicBoolean(false);
        this.executor = Executors.newSingleThreadExecutor();
        this.connectionHandler = new ConnectionHandler(repository);
    }


    public void start()
                    throws IOException
    {
        executor.submit(this::openConnection);
    }


    private void openConnection()
    {
        try (Selector selector = Selector.open();
             SocketChannel clientChannel = SocketChannel.open())
        {
            clientChannel.configureBlocking(false);
            clientChannel.connect(targetAddress);
            clientChannel.register(selector, clientChannel.validOps());

            this.channel = clientChannel;

            running.set(true);
            LOG.info("Client started. Type messages to send:");

            listenOnChannel(selector);
        }
        catch (IOException e)
        {
            LOG.warn("Error starting server communicator", e);
        }
        catch (Exception e)
        {
            LOG.error("Error in server communicator", e);
        }
        finally

        {
            running.set(false);
            executor.shutdown();
        }

    }


    private void listenOnChannel(Selector selector)
                    throws Exception
    {
        while (running.get())
        {
            if (selector.select(1000) == 0)
            {
                continue;
            }

            for (var key : selector.selectedKeys())
            {
                if (key.isWritable())
                {
                    connectionHandler.handleWrite(key);
                }
                else if (key.isReadable())
                {
                    connectionHandler.handleRead(key);
                }
                else if (key.isConnectable())
                {
                    connectionHandler.handleConnect(key);
                }
                else
                {
                    LOG.warn("Unknown key state: {}", key);
                }
            }

            selector.selectedKeys().clear();
        }
    }


    /**
     * A synchronous method to send a message to the server. The message is enqueued and will be sent when the channel is ready for
     * writing.
     *
     * @param message The message to send
     * @return The response from the server
     */
    public String send(String message)
    {
        try
        {
            connectionHandler.enqueueMessage(message);
        }
        catch (Exception e)
        {
            LOG.error("Error when sending message: {}", message, e);
        }
        return null;
    }


    public boolean isRunning()
    {
        return running.get();
    }


    public void download(String address, String file, String downloadDirectory)
    {
        if (channel == null || !channel.isConnected())
        {
            LOG.warn("Channel is not open. Skipping download command...");
            return;
        }

        final var parsedAddress = new InetSocketAddress(address, 8021);
        try
        {
            channel.connect(parsedAddress);

        }
        catch (IOException e)
        {
            LOG.error("Error when download file {} from address {}", file, address);
        }
    }
}
