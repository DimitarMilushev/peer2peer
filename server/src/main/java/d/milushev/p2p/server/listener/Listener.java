package main.java.d.milushev.p2p.server.listener;


import main.java.d.milushev.p2p.server.exceptions.ServerException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Listener implements Runnable, AutoCloseable
{
    private static final Logger LOG = LogManager.getLogger(Listener.class);

    private final int port;

    private volatile boolean isStopped;

    private final ServerSocketChannel serverChannel;
    private final Selector selector;

    private final ActiveConnections connections;
    private final ExecutorService executor;
    private final ConnectionHandler handler;


    public Listener(int port) throws ServerException
    {
        try
        {
            this.serverChannel = ServerSocketChannel.open();
            this.selector = Selector.open();
        }
        catch (IOException e)
        {
            throw new ServerException("Failed to open server socket", e);
        }

        this.port = port;
        isStopped = false;

        connections = new ActiveConnections();
        executor = Executors.newVirtualThreadPerTaskExecutor();
        handler = new ConnectionHandler(connections, executor);
    }


    public void stop()
    {
        this.isStopped = false;
    }


    public boolean isStopped()
    {
        return this.isStopped;
    }


    @Override
    public void run()
    {
        try
        {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            LOG.info("Started..");

            while (!isStopped)
            {
                final int readyChannels = selector.select(1000);

                if (this.isStopped || readyChannels == 0)
                {
                    continue;
                }

                for (var key : selector.selectedKeys())
                {
                    if (!key.isValid())
                    {
                        continue;
                    }

                    if (key.isAcceptable())
                    {
                        handler.handleAccept(key);
                    }
                    else if (key.isReadable())
                    {
                        handler.handleRead(key);
                    }
                    else if (key.isWritable())
                    {
                        handler.handleWrite(key);
                    }
                }

                selector.selectedKeys().clear();
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed during listener startup: {}", e.getMessage(), e);
        }
        catch (ServerException e)
        {
            LOG.error("An internal server error has occurred: {}", e.getMessage(), e);
        }
        finally
        {
            LOG.info("Stopping Listener...");
            stop();
        }
    }


    @Override
    public void close() throws ServerException
    {
        LOG.info("Closing Listener...");

        isStopped = true;

        executor.close();
        closeAllConnections();
        closeServerSocket();
    }


    private void closeServerSocket() throws ServerException
    {
        try
        {
            selector.close();
            serverChannel.close();
        }
        catch (IOException e)
        {
            throw new ServerException("Failed to close server socket", e);
        }
    }


    private void closeAllConnections() throws ServerException
    {
        try
        {
            connections.closeAll();
        }
        catch (IOException e)
        {
            throw new ServerException("Failed to close client connections", e);
        }
    }

}
