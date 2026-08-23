package main.java.d.milushev.p2p.client.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;


public class DownloadHandler
{
    private static final Logger LOG = LogManager.getLogger(DownloadHandler.class);

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;


    public void connect(InetSocketAddress address)
    {
        LOG.info("Connecting to server {}", address);

        if (socket != null && socket.isConnected())
        {
            LOG.info("Already connected to server {}", address);
            return;
        }

        try
        {
            socket = new Socket(address.getHostName(), address.getPort());

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
    }


    public void disconnect()
    {
        try
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
                socket = null;
                LOG.info("Disconnected from server.");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    public void send(String message)
                    throws IOException
    {
        if (socket == null || socket.isClosed())
        {
            LOG.warn("Socket is not connected. Abort sending message [{}]", message);
            return;
        }

        outputStream.write(message.getBytes());
        outputStream.flush();
    }


    public String read()
                    throws IOException
    {
        if (socket == null || socket.isClosed())
        {
            System.out.println("Socket is not connected.");
            return null;
        }

        final byte[] buffer = new byte[1024];
        final int bytesRead = inputStream.read(buffer);

        return new String(buffer, 0, bytesRead);
    }


    public void download(String fileName, String destination)
    {
        if (socket == null || socket.isClosed())
        {
            LOG.warn("Socket is not connected. Aborting download...");
            return;
        }

        final String command = "download " + fileName;
        try
        {
            long fileSize = initialFileRequest(command);
            final Path newFilePath = Path.of(destination).resolve(fileName);
            if (Files.exists(newFilePath))
            {
                throw new Exception("File already exists in downloads directory: " + newFilePath);
            }

            final Path tempFilePath = Files.createTempFile(newFilePath.getParent(), "." + newFilePath.getFileName(), null);
            readDataIntoTempFile(tempFilePath, fileSize);

            LOG.info("Copying data into a full file: {}", newFilePath);
            Files.copy(tempFilePath, newFilePath);

            LOG.info("Removing temp file: {}", tempFilePath);
            Files.delete(tempFilePath);
        }
        catch (Exception e)
        {
            LOG.error("Error during file download", e);
            disconnect();
        }
    }


    private void readDataIntoTempFile(Path tempFile, long fileSize)
                    throws IOException
    {
        if (socket == null || socket.isClosed())
        {
            throw new IOException("Socket is not connected.");
        }

        try (var fileOutputStream = Files.newOutputStream(tempFile))
        {
            final byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead = inputStream.read(buffer);

            while (totalRead < fileSize && bytesRead != -1)
            {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                LOG.info("{}% complete", Math.round(((double)totalRead / fileSize) * 100));

                bytesRead = inputStream.read(buffer);
            }
        }
    }


    private long initialFileRequest(String command)
                    throws NumberFormatException, IOException
    {
        send(command);

        final String response = read();
        if (response == null || response.startsWith("Error"))
        {
            throw new IOException("Failed to get file size from server: " + response);
        }

        final String[] parts = response.split(" ");
        if (parts.length < 2)
        {
            throw new IOException("Invalid response from server: " + response);
        }
        if (!parts[0].equals("OK"))
        {
            throw new IOException("Server did not acknowledge file request: " + response);
        }

        return Long.parseLong(parts[1]);
    }
}
