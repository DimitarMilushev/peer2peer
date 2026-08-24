package main.java.d.milushev.p2p.client.filetransfer.v1;


import main.java.d.milushev.p2p.client.repository.RegisteredFilesRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;


public class FileServer implements Runnable, AutoCloseable
{
    private static final Logger LOG = LogManager.getLogger(FileServer.class);

    private final RegisteredFilesRepository registeredFilesRepository;
    private final AtomicBoolean isRunning;
    private final int port;


    public FileServer(RegisteredFilesRepository registeredFilesRepository, int port)
    {
        this.registeredFilesRepository = registeredFilesRepository;
        this.port = port;
        this.isRunning = new AtomicBoolean(false);
    }


    @Override
    public void run()
    {
        try (final var serverSocket = new ServerSocket(port))
        {
            isRunning.set(true);

            while (isRunning.get())
            {
                listen(serverSocket);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private void listen(ServerSocket serverSocket)
                    throws IOException, InterruptedException
    {
        LOG.info("Listening...");
        final Socket socket = serverSocket.accept();
        final var inputStream = socket.getInputStream();
        final var outputStream = socket.getOutputStream();

        int tries = 0;
        while (inputStream.available() == 0)
        {
            if (tries > 120)
            {
                LOG.info("No data received from client after 10 tries");
                socket.close();
                return;
            }

            Thread.sleep(500);
            ++tries;
        }

        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        final String command = new String(buffer, 0, bytesRead);
        if (command.startsWith("download"))
        {
            handleDownloadCommand(command, inputStream, outputStream, socket);
            return;
        }

        outputStream.write("Invalid command".getBytes());
        socket.close();
    }


    private void handleDownloadCommand(String command, InputStream inputStream, OutputStream outputStream, Socket socket)
                    throws IOException
    {
        final String[] tokens = command.split(" ");
        if (tokens.length != 2)
        {
            outputStream.write("Invalid command format. Usage: download <filename>".getBytes());
            socket.close();
            return;
        }

        final String filename = tokens[1];
        final String path = registeredFilesRepository.getFilePath(filename);
        if (path == null)
        {
            outputStream.write("File not found".getBytes());
            socket.close();
            return;
        }

        final File file = new File(path);
        if (!file.exists())
        {
            outputStream.write("File not found".getBytes());
            socket.close();
            return;
        }

        final long fileSize = file.length();
        outputStream.write(("OK " + fileSize).getBytes());

        if (!isClientReadyForTransfer(inputStream))
        {
            LOG.info("Client abandoned transfer operation.");
            socket.close();
            return;
        }

        final var fileInputStream = new FileInputStream(file);
        final byte[] buffer = new byte[4096];
        long totalRead = 0;
        int bytesRead = fileInputStream.read(buffer);
        while (bytesRead != -1 && socket.isConnected())
        {
            outputStream.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
            LOG.info("{}% sent", Math.round(((double)totalRead / fileSize) * 100));

            bytesRead = fileInputStream.read(buffer);
        }
        outputStream.flush();

        LOG.info("File {} sent to client", filename);
        socket.close();
    }


    private boolean isClientReadyForTransfer(InputStream inputStream)
                    throws IOException
    {
        final byte[] buffer = new byte[1024];
        final int bytesRead = inputStream.read(buffer);

        final String response = new String(buffer, 0, bytesRead);
        LOG.debug("Client ready for transfer response {}", response);

        return response.startsWith("OK");
    }


    @Override
    public void close()
    {
        LOG.info("Closing gracefully...");
        isRunning.set(false);
    }
}
