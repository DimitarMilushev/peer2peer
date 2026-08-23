package main.java.d.milushev.p2p.client.filetransfer;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;


public class FileTransferServer implements Runnable
{
    private static final Logger LOG = LogManager.getLogger(FileTransferServer.class);

    private final AtomicBoolean isStopped;

    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final int port;


    public FileTransferServer(int port)
                    throws Exception
    {
        this.port = port;

        try
        {
            this.serverChannel = ServerSocketChannel.open();
            this.selector = Selector.open();
            prepareDirectories();
        }
        catch (IOException e)
        {
            throw new Exception("Failed to initialize FileTransferServer", e);
        }

        isStopped = new AtomicBoolean(false);
    }


    private void prepareDirectories()
    {
        final Path uploadsDir = Paths.get(System.getProperty("user.home"), ".p2p", "uploads");
        if (!uploadsDir.toFile().exists())
        {
            final boolean success = uploadsDir.toFile().mkdirs();
            if (!success)
            {
                LOG.error("Failed to create uploads directory: {}", uploadsDir);
            }
        }

        final Path downloadsDir = Paths.get(System.getProperty("user.home"), ".p2p", "downloads");
        if (!downloadsDir.toFile().exists())
        {
            final boolean success = downloadsDir.toFile().mkdirs();
            if (!success)
            {
                LOG.error("Failed to create downloads directory: {}", downloadsDir);
            }
        }
    }


    @Override
    public void run()
    {
        try
        {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            LOG.info("FileTransferServer started on port: {}", port);

            while (!isStopped.get())
            {
                final int readyChannels = selector.select(1000);

                if (this.isStopped.get() || readyChannels == 0)
                {
                    continue;
                }

                try
                {
                    for (var key : selector.selectedKeys())
                    {
                        if (!key.isValid())
                        {
                            continue;
                        }

                        if (key.isAcceptable())
                        {
                            handleAccept(key);
                        }

                        if (key.isReadable())
                        {
                            handleRead(key);
                        }
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Unexpected error: {}", e.getMessage(), e);
                }
            }
        }
        catch (IOException e)
        {
            LOG.error("Error in FileTransferServer: ", e);
        }
    }


    private void handleRead(SelectionKey key)
                    throws Exception
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            final var socket = clientChannel.socket();
            LOG.debug("Handling read for [{}]", socket.getRemoteSocketAddress());

            final FileTransferUploadHandler handler = new FileTransferUploadHandler();
            handler.handleFileTransfer(key.selector());
            LOG.debug("READ finished for [{}]: {}", socket.getRemoteSocketAddress());
            return;
        }

        throw new Exception("Invalid channel was opened for READ operation");
    }


    private void handleAccept(SelectionKey key)
                    throws Exception
    {
        if (key.channel() instanceof ServerSocketChannel serverChannel)
        {
            LOG.info("Handling ACCEPT");
            final var channel = serverChannel.accept();
            final var socket = channel.socket();

            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_READ);
            key.attach(ByteBuffer.allocate(1024));

            LOG.info("Successfully accepted client channel [{}]", socket.getRemoteSocketAddress());
            return;
        }

        throw new Exception("Invalid channel was opened for ACCEPT operation");
    }
}
