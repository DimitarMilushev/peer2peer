package main.java.d.milushev.p2p.server.listener;


import main.java.d.milushev.p2p.server.exceptions.ServerException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Listener implements Runnable, AutoCloseable
{
    private static final Logger LOGGER = Logger.getGlobal();

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

            System.out.println("Started..");

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
            System.out.println("Failed during listener startup: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed during listener startup: " + e.getMessage(), e);
        }
        catch (ServerException e)
        {
            System.out.println("An internal error has occurred: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "An internal server error has occurred: " + e.getMessage(), e);
        }
        finally
        {
            System.out.println("Stopping Listener...");
            stop();
        }
    }


    @Override
    public void close() throws ServerException
    {
        System.out.println("Closing Listener...");

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
