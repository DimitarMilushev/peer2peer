package main.java.d.milushev.p2p.server.commands;


import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;

import java.net.Socket;
import java.util.function.Consumer;

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
            socket.close();
            onCloseUser.accept(socket);
            final var removed = repository.removeByAddress(socket.getRemoteSocketAddress().toString());

            LOG.info("Closed users [{}]", removed);
        }
        catch (Exception e)
        {
            LOG.error("Error during UnregisterUserCommand command: {}", e.getMessage(), e);
        }
    }
}
