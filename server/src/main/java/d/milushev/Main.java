package main.java.d.milushev;


import main.java.d.milushev.server.Listener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Main
{
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());


    public static void main(String[] args) throws InterruptedException
    {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false); // Prevent double logging

        final AtomicBoolean stopSignal = new AtomicBoolean(false);

        try (final var executor = Executors.newVirtualThreadPerTaskExecutor())
        {
            final var processor = new Processor(stopSignal);
            executor.submit(processor);

            final var server = new Listener(8000, processor);

            executor.submit(server);
            executor.submit(new ConsoleInputListener(stopSignal));

            while (server.isActive() && !stopSignal.get())
            {
                Thread.sleep(1000);
            }

            stopSignal.set(true);
            server.close();
            executor.shutdown();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }
}
