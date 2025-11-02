package main.java.d.milushev.p2p.server.commands;


import main.java.d.milushev.p2p.server.repository.InMemoryClientsRepository;

import java.net.Socket;


public class CloseUserCommand implements Command
{
    private final Socket socket;
    private final InMemoryClientsRepository repository;


    public CloseUserCommand(Socket socket, InMemoryClientsRepository repository)
    {
        this.socket = socket;
        this.repository = repository;
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
        }
    }
}
