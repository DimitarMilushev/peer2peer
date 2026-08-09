package main.java.d.milushev.p2p.client;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


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
    }
}
