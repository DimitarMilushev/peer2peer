package main.java.d.milushev.p2p.client.filetransfer;


import d.milushev.p2p.network_utils.BufferUtils;
import d.milushev.p2p.network_utils.SocketUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;


public class FileTransferUploadHandler
{
    private static final Logger LOG = LogManager.getLogger(FileTransferUploadHandler.class);

    private FileTransferState state;
    private File targetFile;


    public FileTransferUploadHandler()
    {
        this.state = FileTransferState.IDLE;
    }


    public void handleFileTransfer(Selector selector)
    {
        state = FileTransferState.CONNECTED;

        while (selector.isOpen())
        {
            if (state == FileTransferState.FINISHED)
            {
                LOG.info("File transfer completed successfully. Closing selector.");
                try
                {
                    selector.close();
                }
                catch (IOException e)
                {
                    LOG.error("Error while closing selector after successful file transfer.", e);
                }
                return;
            }

            for (var key : selector.selectedKeys())
            {
                if (key.isReadable())
                {
                    try
                    {
                        handleRead(key);
                    }
                    catch (Exception e)
                    {
                        LOG.error("Error while handling READ for channel: {}", key.channel(), e);
                        return;
                    }
                }
                else if (key.isWritable())
                {
                    try
                    {
                        handleWrite(key);
                    }
                    catch (Exception e)
                    {
                        LOG.error("Error while handling WRITE for channel: {}", key.channel(), e);
                        return;
                    }
                }
                else
                {
                    LOG.error("Unknown key state: {}", key);
                }
            }
        }
    }


    private void handleWrite(SelectionKey key)
                    throws IOException
    {
        switch (state)
        {
            case FILE_CHECK -> respondWithFile(key);
            case FILE_CONFIRMED -> sendFileData(key);
            default -> LOG.error("Unknown state: {}", state);
        }
    }


    private void respondWithFile(SelectionKey key)
                    throws IOException
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            if (targetFile == null || !targetFile.exists())
            {
                LOG.error("Target file is not set or does not exist.");
                SocketUtils.writeToChannel(clientChannel, ByteBuffer.allocate(1024), "ERROR: File not found");
                state = FileTransferState.FINISHED;
                return;
            }

            final String response = "OK " + Files.size(targetFile.toPath());
            SocketUtils.writeToChannel(clientChannel, ByteBuffer.allocate(1024), response);
            state = FileTransferState.FILE_CONFIRMED;
            key.interestOps(SelectionKey.OP_READ);
        }
        else
        {
            LOG.error("Invalid channel type for file check response: {}", key.channel());
        }
    }


    private void handleRead(SelectionKey key)
    {
        switch (state)
        {
            case CONNECTED -> prepareFile(key);
            case FILE_CONFIRMED -> sendFileData(key);
            default -> LOG.error("Unknown state: {}", state);
        }
    }


    private void sendFileData(SelectionKey key)
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            try (var fileChannel = FileChannel.open(targetFile.toPath()))
            {
                long position = 0;
                long size = fileChannel.size();
                long transferred;

                while (position < size)
                {
                    transferred = fileChannel.transferTo(position, size - position, clientChannel);
                    if (transferred <= 0)
                    {
                        break;
                    }
                    position += transferred;
                }

                LOG.info("File transfer completed successfully for channel: {}", clientChannel);
                state = FileTransferState.FINISHED;
            }
            catch (IOException e)
            {
                LOG.error("Error while sending file data to channel: {}", clientChannel, e);
                state = FileTransferState.FINISHED;
            }
        }
        else
        {
            LOG.error("Invalid channel type for sending file data: {}", key.channel());
        }
    }


    private void prepareFile(SelectionKey key)
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            final ByteBuffer buffer = ByteBuffer.allocate(1024);
            final String response = BufferUtils.bufferToString(buffer, buffer.position());

            targetFile = new File(response);
            state = FileTransferState.FILE_CHECK;
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else
        {
            LOG.error("Invalid channel type for handshake: {}", key.channel());
        }
    }
}
