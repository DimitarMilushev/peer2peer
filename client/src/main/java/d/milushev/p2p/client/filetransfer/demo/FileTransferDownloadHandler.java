package main.java.d.milushev.p2p.client.filetransfer;


import main.java.d.milushev.p2p.client.repository.models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;


/**
 * A primitive implementation of the file transfer download handler. This class is responsible for handling the file transfer connections
 * and managing the file transfer process.
 */
public class FileTransferDownloadHandler
{
    private static final Logger LOG = LogManager.getLogger(FileTransferDownloadHandler.class);
    private FileTransferState state;
    private int fileSize;


    public FileTransferDownloadHandler()
    {
        this.state = FileTransferState.IDLE;
    }


    public void handleFileTransfer(User user, String filePath)
    {
        if (user == null || filePath == null || filePath.isBlank())
        {
            LOG.error("Invalid user or file path for file transfer. User: {}, File Path: {}", user, filePath);
            return;
        }

        if (user.address() == null || user.address().isBlank())
        {
            LOG.error("User address is null or blank for file transfer. User: {}", user);
            return;
        }

        final String[] userAddress = user.address().split(":");
        if (userAddress.length != 2 || !isPort(userAddress[1]))
        {
            LOG.error("Invalid user address format for file transfer. User: {}", user);
            return;
        }

        final String host = userAddress[0];
        final int port = Integer.parseInt(userAddress[1]);

        final InetSocketAddress targetAddress = new InetSocketAddress(host, port);
        try (SocketChannel channel = SocketChannel.open(targetAddress);
             Selector selector = Selector.open())
        {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            LOG.info("Initiating file transfer to: {} for user: {}", targetAddress, user);

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
                            handleWrite(key, filePath);
                        }
                        catch (Exception e)
                        {
                            LOG.error("Error while handling WRITE for channel: {}", key.channel(), e);
                            return;
                        }
                    }
                    else if (key.isConnectable())
                    {
                        handleConnect(key, targetAddress);
                    }
                    else
                    {
                        LOG.error("Unknown key state: {}", key);
                    }
                }
            }
        }
        catch (IOException e)
        {
            LOG.error("Error while initiating file transfer to: {} for user: {}", targetAddress, user, e);
        }
    }


    private void handleWrite(SelectionKey key, String filePath)
    {
        if (state == FileTransferState.CONNECTED)
        {
            handleCheckFile(key, filePath);
            return;
        }

        if (state == FileTransferState.FILE_CONFIRMED)
        {
            confirmDownload(key);
        }
    }


    private void confirmDownload(SelectionKey key)
    {
        if (key.channel() instanceof SocketChannel channel)
        {
            try
            {
                channel.write(ByteBuffer.wrap("READY".getBytes()));
                LOG.info("Sent READY signal to channel: {}", channel);
            }
            catch (IOException e)
            {
                LOG.error("Error while sending READY signal to channel: {}", channel, e);
            }
        }
    }


    private void handleRead(SelectionKey key)
                    throws IOException
    {
        switch (state)
        {
            case FILE_CHECK -> handleFileCheckResponse(key);
            case FILE_CONFIRMED -> handleFileDownload(key);
            default -> LOG.warn("Received READ event in unexpected state: {}", state);
        }
    }


    private void handleFileDownload(SelectionKey key)
    {
        if (key.channel() instanceof SocketChannel sourceChannel)
        {
            final String dummyPath = "C:\\Users\\d.milushev\\Downloads\\testfile.txt"; // Replace with the desired destination path
            if (Files.exists(new File(dummyPath).toPath()))
            {
                LOG.warn("File already exists at destination path: {}. Overwriting.", dummyPath);
                try
                {
                    Files.delete(new File(dummyPath).toPath());
                }
                catch (IOException e)
                {
                    LOG.error("Error while deleting existing file at destination path: {}", dummyPath, e);
                }
            }

            try
            {
                final File received = Files.createFile(new File(dummyPath).toPath()).toFile();
                try (FileChannel fileChannel = FileChannel.open(received.toPath()))
                {
                    long bytesTransferred = 0;
                    while (bytesTransferred < fileSize)
                    {
                        bytesTransferred += fileChannel.transferFrom(sourceChannel, bytesTransferred, fileSize - bytesTransferred);
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Error while receiving file to: {}", dummyPath, e);
                }

                LOG.info("File received and saved to: {}", received.getAbsolutePath());
                state = FileTransferState.FINISHED; // Reset state after file transfer
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    private void handleFileCheckResponse(SelectionKey key)
                    throws IOException
    {
        if (key.channel() instanceof SocketChannel channel)
        {
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            int bytesRead = channel.read(buffer);
            if (bytesRead == -1)
            {
                LOG.error("End of stream reached while reading file check response from channel: {}", channel);
                return;
            }

            buffer.flip();
            String response = new String(buffer.array(), 0, bytesRead).trim();
            LOG.info("Received file check response: {} from channel: {}", response, channel);

            if (response.startsWith("OK"))
            {
                state = FileTransferState.FILE_CONFIRMED;
                //TODO: Handle the file size from the response if needed
                this.fileSize = Integer.parseInt(response.split(" ")[1]);
                LOG.info("File transfer confirmed. Ready to send the file.");
            }
            else
            {
                LOG.error("File transfer denied by the recipient. Response: {}", response);
                state = FileTransferState.FINISHED; // Reset state after denial
                throw new IOException("File transfer denied by the recipient. Response: " + response);
            }
        }
    }


    private void handleCheckFile(SelectionKey key, String filePath)
    {
        if (key.channel() instanceof SocketChannel channel)
        {
            try
            {
                channel.write(ByteBuffer.wrap(filePath.getBytes()));
                state = FileTransferState.FILE_CHECK;

                LOG.info("Sent file path: {} to channel: {}", filePath, channel);
            }
            catch (IOException e)
            {
                LOG.error("Error while sending file path: {} to channel: {}", filePath, channel, e);
            }
        }
    }


    private void handleConnect(SelectionKey key, InetSocketAddress targetAddress)
    {
        if (key.channel() instanceof SocketChannel channel && state == FileTransferState.IDLE)
        {
            try
            {
                channel.connect(targetAddress);
                LOG.info("Connected to target address: {}", targetAddress);
                state = FileTransferState.CONNECTED;
            }
            catch (IOException e)
            {
                LOG.error("Error while connecting to target address: {}", targetAddress, e);
            }
        }
    }


    private boolean isPort(String port)
    {
        try
        {
            int portNumber = Integer.parseInt(port);
            return portNumber >= 0 && portNumber <= 65535;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
