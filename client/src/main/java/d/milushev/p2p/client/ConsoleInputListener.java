package main.java.d.milushev.p2p.client;


import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import main.java.d.milushev.p2p.client.repository.RegisteredFilesRepository;
import main.java.d.milushev.p2p.client.repository.models.User;
import main.java.d.milushev.p2p.client.server.ServerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConsoleInputListener implements Runnable, AutoCloseable
{
    private static final Logger LOG = LogManager.getLogger(ConsoleInputListener.class);

    private final AtomicBoolean isStopped;
    private final ActiveUsersRepository repository;
    private final ServerCommunicator communicator;
    private final RegisteredFilesRepository filesRepository;

    public ConsoleInputListener(AtomicBoolean stopSignal, ActiveUsersRepository repository, RegisteredFilesRepository filesRepository)
    {
        this.isStopped = stopSignal;
        this.repository = repository;
        this.filesRepository = filesRepository;
        this.communicator = new ServerCommunicator("localhost", 8000, repository);
    }


    @Override
    public void run()
    {
        try
        {
            communicator.start();
        }
        catch (IOException e)
        {
            LOG.error(e);
            throw new RuntimeException(e);
        }

        final var scn = new Scanner(System.in);

        String inputString = "";
        while (!inputString.equals("stop") && !isStopped.get())
        {
            if (scn.hasNextLine())
            {
                inputString = scn.nextLine();
                if (inputString.isEmpty())
                {
                    continue;
                }
                final String command = inputString.split(" ")[0];
                try
                {
                    switch (command)
                    {
                        case "register" -> handleRegisterCommand(inputString);
                        case "unregister" -> handleUnregisterCommand(inputString);
                        case "download" -> handleDownloadCommand(inputString);
                        default -> communicator.send(inputString);
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Error while sending command to server: ", e);
                }
            }
        }

        this.isStopped.set(true);
    }


    private void handleDownloadCommand(String inputString)
    {
        final String[] parts = inputString.split(" ");
        if (parts.length != 4)
        {
            LOG.info("Malformed download command {}", inputString);
            return;
        }
        // Find user in repo
        final String username = parts[1];
        final User user = repository.getByUsername(username);
        if (user == null)
        {
            LOG.info("User {} is not found", username);
            return;
        }

        final String host = user.address().split(":")[0];
        final String file = parts[2];
        final String destination = parts[3];
        // call download with new address, filename and downloadFolderPath
        communicator.download(host, file, destination);
    }


    private void handleUnregisterCommand(String inputString)
    {
        final String[] parts = inputString.split(" ");
        if (parts.length < 3)
        {
            LOG.info("Malformed unregister command {}", inputString);
            return;
        }

        final Set<String> unregisteredFiles = new HashSet<>(parts.length - 2);
        for (int i = 2; i < parts.length; i++)
        {
            final String fileName = parts[i];

            if (!filesRepository.hasFile(fileName))
            {
                LOG.info("File {} was not found.", fileName);
                continue;
            }

            filesRepository.removeFile(fileName);
            unregisteredFiles.add(fileName);
            LOG.info("Unregistered file {}", fileName);
        }

        if (unregisteredFiles.isEmpty())
        {
            LOG.info("Nothing to unregister...");
            return;
        }

        final String username = parts[1];
        communicator.send("unregister " + username + " " + String.join(" ", unregisteredFiles));
    }


    private void handleRegisterCommand(String inputString)
    {
        final String[] parts = inputString.split(" ");
        if (parts.length < 3)
        {
            LOG.info("Malformed register command {}", inputString);
            return;
        }

        final Set<String> registeredFiles = HashSet.newHashSet(parts.length - 2);
        for (int i = 2; i < parts.length; i++)
        {
            final Path filePath = Path.of(parts[i]);
            if (!Files.exists(filePath))
            {
                LOG.info("File [{}] does not exist", filePath);
                return;
            }

            if (filesRepository.hasFile(filePath.getFileName().toString()))
            {
                LOG.info("File [{}] is already registered", filePath);
                continue;
            }

            filesRepository.addFile(filePath.getFileName().toString(), filePath.toString());
            registeredFiles.add(filePath.getFileName().toString());
            LOG.info("Registered {}", filePath);
        }

        if (registeredFiles.isEmpty())
        {
            LOG.info("Nothing to register...");
            return;
        }

        final String username = parts[1];
        communicator.send("register " + username + " " + String.join(" ", registeredFiles));
    }


    @Override
    public void close()
    {
        LOG.info("Closing Console...");

        this.isStopped.set(true);
    }
}
