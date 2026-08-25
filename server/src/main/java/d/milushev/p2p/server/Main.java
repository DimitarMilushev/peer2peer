package main.java.d.milushev.p2p.server;


import main.java.d.milushev.p2p.server.env.EnvProperties;
import main.java.d.milushev.p2p.server.exceptions.ServerException;
import main.java.d.milushev.p2p.server.listener.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class Main
{
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException
    {
        LOG.info("Starting server...");

        final AtomicBoolean stopSignal = new AtomicBoolean(false);

        try (final var executor = Executors.newFixedThreadPool(2);
                        final var console = new ConsoleInputListener(stopSignal);
                        final var listener = new Listener(EnvProperties.SERVER_PORT.getOrDefault()))
        {
            executor.submit(listener);
            executor.submit(console);

            while (!listener.isStopped() && !stopSignal.get())
            {
                Thread.sleep(1000);
            }


            LOG.info("Closing resources...");
        }
        catch (ServerException e)
        {
            LOG.error("Exception has occurred during server runtime", e);
        }

        LOG.info("Server stopped");
    }
}
