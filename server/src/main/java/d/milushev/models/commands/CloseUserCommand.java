package main.java.d.milushev.models.commands;


import main.java.d.milushev.repository.InMemoryClientsRepository;

import java.net.Socket;
import java.util.Queue;


public class CloseUserCommand implements Command
{
    public static final String NAME = "close-user";
    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Queue<Exception> errors;


    public CloseUserCommand(Socket socket, InMemoryClientsRepository repository, Queue<Exception> errors)
    {
        this.socket = socket;
        this.repository = repository;
        this.errors = errors;
    }


    @Override
    public void run()
    {
        try
        {
            final var removed = repository.removeByAddress(socket.getRemoteSocketAddress().toString());
            System.out.println("Removed users [" + removed + "]");
        }
        catch (Exception e)
        {
            System.out.println("Error during UnregisterUserCommand command [" + e.getMessage() + "]");
            e.printStackTrace();
            this.errors.add(e);
        }
    }
}
