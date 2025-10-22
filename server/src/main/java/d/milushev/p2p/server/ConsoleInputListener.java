package main.java.d.milushev.p2p.server;


import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConsoleInputListener implements Runnable
{
    private final AtomicBoolean isStopped;

    public ConsoleInputListener(AtomicBoolean isStopped)
    {
        this.isStopped = isStopped;
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
}
