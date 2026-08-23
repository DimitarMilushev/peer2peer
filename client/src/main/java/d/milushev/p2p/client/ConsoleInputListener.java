package main.java.d.milushev.p2p.client;


import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import main.java.d.milushev.p2p.client.server.ServerCommunicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConsoleInputListener implements Runnable, AutoCloseable
{
    private static final Logger LOG = LogManager.getLogger(ConsoleInputListener.class);
    private AtomicBoolean isStopped;
    private ActiveUsersRepository repository;

    public ConsoleInputListener(AtomicBoolean stopSignal, ActiveUsersRepository repository)
    {
        this.isStopped = stopSignal;
        this.repository = repository;
    }

    @Override
    public void run()
    {
        final ServerCommunicator communicator = new ServerCommunicator("localhost", 8000, repository);
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
                try
                {
                    communicator.send(inputString);
                }
                catch (Exception e)
                {
                    LOG.error("Error while sending command to server: ", e);
                }
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
