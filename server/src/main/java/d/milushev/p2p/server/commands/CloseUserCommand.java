package main.java.d.milushev.p2p.server.commands;


import main.java.d.milushev.p2p.server.exceptions.repository.EntityNotFoundException;
import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import main.java.d.milushev.p2p.server.repositories.models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CloseUserCommand implements Command
{
    public static final String NAME = "close-user";

    private static final Logger LOG = LogManager.getLogger(CloseUserCommand.class);

    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Consumer<Socket> onCloseUser;


    public CloseUserCommand(Socket socket, InMemoryClientsRepository repository, Consumer<Socket> onCloseUser)
    {
        this.socket = socket;
        this.repository = repository;
        this.onCloseUser = onCloseUser;
    }


    @Override
    public void run()
    {
        try
        {
            onCloseUser.accept(socket);
            final List<User> removed = removeUsersByAddress(socket.getRemoteSocketAddress());
            LOG.info("Closed users {}", removed);
        }
        catch (Exception e)
        {
            LOG.error("Error during UnregisterUserCommand command: {}", e.getMessage(), e);
        }
    }


    private List<User> removeUsersByAddress(SocketAddress address)
    {
        try
        {
            return repository.removeByAddress(address.toString());
        }
        catch (EntityNotFoundException e)
        {
            LOG.debug("Failed to find any users by IP {}: {}", address, e.getMessage());
            return Collections.emptyList();
        }
    }
}
