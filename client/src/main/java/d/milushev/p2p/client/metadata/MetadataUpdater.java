package main.java.d.milushev.p2p.client.metadata;


import main.java.d.milushev.p2p.client.env.EnvProperties;
import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import main.java.d.milushev.p2p.client.server.ServerCommunicator;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Updates the metadata of the client based on the server responses. This class is responsible for parsing the server responses and updating
 * the local metadata accordingly.
 */
public class MetadataUpdater implements Runnable
{
    private static final String METADATA_COMMAND = "list-active-users";

    private static final Logger LOG = LogManager.getLogger(MetadataUpdater.class);

    private final int refreshTimeout;
    private final ServerCommunicator serverCommunicator;
    private final AtomicBoolean isRunning;
    private final ActiveUsersRepository repository;


    public MetadataUpdater(ActiveUsersRepository repository)
    {
        this.serverCommunicator = new ServerCommunicator(
                        EnvProperties.SERVER_HOST.getOrDefault(),
                        EnvProperties.SERVER_PORT.getOrDefault(),
                        repository
        );

        this.isRunning = new AtomicBoolean(false);
        this.repository = repository;
        this.refreshTimeout = EnvProperties.UPDATER_TIMEOUT_S.getOrDefault();
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
            Thread.sleep(Duration.ofSeconds(refreshTimeout).toMillis());
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
            final String response = serverCommunicator.sendSync(METADATA_COMMAND);
            LOG.info("Received metadata from server: {}", response);

            if (response == null || response.isBlank() || response.equals("[]"))
            {
                return;
            }

            final var users = MetadataParser.parseUsers(response);
            LOG.info("Parsed users {}", users);

            if (users.isEmpty())
            {
                return;
            }

            repository.addAll(users);
        }
        catch (Exception e)
        {
            LOG.error("Error refreshing metadata", e);
        }
    }
}
