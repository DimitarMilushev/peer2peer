package main.java.d.milushev.p2p.server.commands;


import d.milushev.p2p.network_utils.factories.ResponseFactory;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.exceptions.ClientException;
import main.java.d.milushev.p2p.server.exceptions.command.CommandException;
import main.java.d.milushev.p2p.server.exceptions.command.InvalidCommandException;
import main.java.d.milushev.p2p.server.exceptions.command.MissingArgumentsCommandException;
import main.java.d.milushev.p2p.server.exceptions.repository.EntityAlreadyExistsException;
import main.java.d.milushev.p2p.server.exceptions.repository.EntityNotFoundException;
import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;
import main.java.d.milushev.p2p.server.repositories.models.User;

import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;


/**
 * This Command should allow clients to update their available files. The "username" parameter associates the string with the actual IP of
 * the connection.
 */
public class RegisterCommand implements Command
{
    private static final Logger LOG = LogManager.getLogger(RegisterCommand.class);
    private static final int MIN_COMMAND_ARGUMENTS = 2;

    private final String input;
    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Queue<ResponseFuture> responses;


    public RegisterCommand(String input, Socket socket, InMemoryClientsRepository repository, Queue<ResponseFuture> responses)
    {
        this.input = input;
        this.socket = socket;
        this.repository = repository;
        this.responses = responses;
    }


    @Override
    public void run()
    {
        final ResponseFuture future = new ResponseFuture(socket.getChannel(), new CompletableFuture<>());

        try
        {
            responses.add(future);
            final User user = parseUser(input, socket.getRemoteSocketAddress().toString());

            final User result = register(user);
            future.response().complete(ResponseFactory.createSuccess(result, socket.getChannel()));
        }
        catch (ClientException e)
        {
            LOG.info("Client error during RegisterClient command", e);

            future.response().complete(ResponseFactory.createClientError(e, socket.getChannel()));
        }
    }


    private User register(User user) throws ClientException
    {
        if (repository.exists(user.name()))
        {
            try
            {
                return repository.addFilesByUsername(user.name(), user.filePaths());
            }
            catch (EntityNotFoundException | EntityAlreadyExistsException e)
            {
                throw new ClientException(
                        "Error occurred while registering files for user [" + user.name() + "]",
                        e,
                        null,
                        socket.getRemoteSocketAddress().toString()
                );
            }
        }

        return repository.addUser(user);
    }


    private User parseUser(String input, String address) throws ClientException
    {
        final String[] tokens = input.split(" ");

        try
        {
            if (tokens.length < MIN_COMMAND_ARGUMENTS)
            {
                throw new MissingArgumentsCommandException("Missing arguments [" + input + "]");
            }

            //TODO: check if not server error???
            if (!tokens[0].equalsIgnoreCase("register"))
            {
                throw new InvalidCommandException("Invalid command [" + tokens[0] + "]");
            }
        }
        catch (CommandException e)
        {
            throw new ClientException(
                    "Error while parsing input [" + input + "]",
                    e,
                    null,
                    socket.getRemoteSocketAddress().toString()
            );
        }

        final String username = tokens[1];
        final String[] filePaths = Arrays.stream(tokens).skip(2).toArray(String[]::new);
        return new User(username, address, new HashSet<>(List.of(filePaths)));
    }
}
