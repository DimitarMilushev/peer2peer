package main.java.d.milushev.p2p.server;


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

        try (final var executor = Executors.newFixedThreadPool(2);
                        final var console = new ConsoleInputListener(stopSignal);
                        final var listener = new Listener(8000))
        {
            executor.submit(listener);
            executor.submit(console);

            while (!listener.isStopped() && !stopSignal.get())
            {
                Thread.sleep(1000);
            }

            System.out.println("Closing resources...");
        }
        catch (IOException e)
        {
            System.out.println("Exception has occurred during server runtime: " + e);
            e.printStackTrace();
        }

        System.out.println("Server stopped");
    }
}
