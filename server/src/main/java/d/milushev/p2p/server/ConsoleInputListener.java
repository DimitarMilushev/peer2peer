package main.java.d.milushev.p2p.server;


import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ConsoleInputListener implements Runnable, AutoCloseable
{
    private static final Logger LOG = LogManager.getLogger(ConsoleInputListener.class);
    private AtomicBoolean isStopped;

    public ConsoleInputListener(AtomicBoolean stopSignal)
    {
        this.isStopped = stopSignal;
    }

    @Override
    public void run()
    {
        final var scn = new Scanner(System.in);

        String inputString = "";
        while (!inputString.equals("stop") && !isStopped.get())
        {
            if (scn.hasNextLine())
            {
                inputString = scn.nextLine();
            }
        }

        this.isStopped.set(true);
    }


    @Override
    public void close()
    {
        LOG.info("Closing Console...");

        this.isStopped.set(true);

        try
        {
            Thread.currentThread().join();
        }
        catch (InterruptedException e)
        {
            LOG.error("Error while waiting for console thread to finish: {}", e.getMessage(), e);
        }
    }
}
