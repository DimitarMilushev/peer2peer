package main.java.d.milushev.p2p.client.metadata;


import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import main.java.d.milushev.p2p.client.server.ServerCommunicator;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Updates the metadata of the client based on the server responses. This class is responsible for parsing the server responses and updating
 * the local metadata accordingly.
 */
public class MetadataUpdater implements Runnable
{
    private static final int DEFAULT_UPDATE_TIMEOUT_S = 30;
    private static final String METADATA_COMMAND = "list-active-users";

    private static final Logger LOG = LogManager.getLogger(MetadataUpdater.class);

    private final ServerCommunicator serverCommunicator;
    private final AtomicBoolean isRunning;
    private final ActiveUsersRepository repository;


    public MetadataUpdater(ActiveUsersRepository repository)
    {
        serverCommunicator = new ServerCommunicator("localhost", 8000);
        isRunning = new AtomicBoolean(false);
        this.repository = repository;
    }


    @Override
    public void run()
    {
        try
        {
            serverCommunicator.start();
        }
        catch (IOException e)
        {
            LOG.error("Error starting server communicator", e);
        }
        isRunning.set(true);

        while (isRunning.get())
        {
            refreshMetadata();
            sleep();
        }
    }


    private void sleep()
    {
        try
        {
            Thread.sleep(DEFAULT_UPDATE_TIMEOUT_S * 1000L);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    private void refreshMetadata()
    {
        if (!serverCommunicator.isRunning())
        {
            LOG.warn("Server communicator is not running. Skipping metadata refresh.");
            return;
        }

        try
        {
            final String response = serverCommunicator.send(METADATA_COMMAND);

            final var users = MetadataParser.parseUsers(response);
            if (!users.isEmpty())
            {
                repository.addAll(users);
            }

            if (response != null && !response.isBlank())
            {
                LOG.info("Received metadata from server: {}", response);
            }
        }
        catch (Exception e)
        {
            LOG.error("Error refreshing metadata", e);
        }
    }
}
