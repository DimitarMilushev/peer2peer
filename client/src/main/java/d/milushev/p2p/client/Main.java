package main.java.d.milushev.p2p.client;


import main.java.d.milushev.p2p.client.metadata.MetadataUpdater;
import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class Main
{
    private static final Logger LOG = LogManager.getLogger(Main.class);


    public static void main(String[] args)
    {
        LOG.info("Starting P2P Client...");
        final AtomicBoolean stopSignal = new AtomicBoolean(false);
        final ActiveUsersRepository repository = new ActiveUsersRepository();

        try (final var executor = Executors.newFixedThreadPool(3);
             final var console = new ConsoleInputListener(stopSignal);
        )
        {
            executor.submit(console);
            executor.submit(new MetadataUpdater(repository));

            LOG.info("Started P2P Client...");
            while (!stopSignal.get())
            {
                Thread.sleep(1000);
            }

            LOG.info("Stopping P2P Client...");
        }
        catch (InterruptedException e)
        {
            LOG.error("P2P Client interrupted", e);
            throw new RuntimeException(e);
        }

        LOG.info("P2P Client has been stopped.");
    }
}
