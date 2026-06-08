package main.java.d.milushev.p2p.server.commands;


import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;

import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CloseUserCommand implements Command
{
    private static final Logger LOG = LogManager.getLogger(CloseUserCommand.class);
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
            LOG.info("Removed users [{}]", removed);
        }
        catch (Exception e)
        {
            LOG.error("Error during UnregisterUserCommand command: {}", e.getMessage(), e);
        }
    }
}
