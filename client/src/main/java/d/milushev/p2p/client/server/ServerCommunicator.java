package main.java.d.milushev.p2p.client.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
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


    public ServerCommunicator(String host, int port, ActiveUsersRepository repository)
    {
        this.targetAddress = new InetSocketAddress(host, port);
        this.running = new AtomicBoolean(false);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.connectionHandler = new ConnectionHandler(repository);
    }


    public void start()
                    throws IOException
    {
        executor.submit(this::openConnection);
    }


    private void openConnection()
    {
        LOG.info("Connecting to {}", targetAddress);

        try (Selector selector = Selector.open();
             SocketChannel clientChannel = SocketChannel.open())
        {
            clientChannel.configureBlocking(false);
            clientChannel.connect(targetAddress);
            clientChannel.register(selector, clientChannel.validOps());

            running.set(true);
            LOG.info("Connection successful");

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
            LOG.info("Connection closed");
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
    public void send(String message)
    {
        try
        {
            connectionHandler.enqueueMessage(message);
        }
        catch (Exception e)
        {
            LOG.error("Error when sending message: {}", message, e);
        }
    }


    public String sendSync(String message)
    {
        send(message);

        try
        {
            int tries = 0;
            while (connectionHandler.getResponse() == null && tries < 3)
            {
                Thread.sleep(500);
                ++tries;
            }

            return connectionHandler.getResponse().get();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get response", e);
            return null;
        }
    }


    public boolean isRunning()
    {
        return running.get();
    }


    public void download(String host, String file, String destination)
    {
        final DownloadHandler handler = new DownloadHandler();

        try
        {
            final InetSocketAddress address = new InetSocketAddress(host, 8021);
            handler.downloadFile(address, file, destination);
        }
        catch (Exception e)
        {
            LOG.error("Failed to handle download", e);
        }
    }
}
