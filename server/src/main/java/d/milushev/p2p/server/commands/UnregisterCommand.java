package main.java.d.milushev.p2p.server.commands;


import d.milushev.p2p.network_utils.factories.ResponseFactory;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.exceptions.ClientException;
import main.java.d.milushev.p2p.server.exceptions.database.EntityNotFoundException;
import main.java.d.milushev.p2p.server.repository.InMemoryClientsRepository;
import main.java.d.milushev.p2p.server.repository.models.User;

import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


public class UnregisterCommand implements Command
{
    private static final int MIN_COMMAND_ARGUMENTS = 2;

    private final String input;
    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Queue<ResponseFuture> responses;


    public UnregisterCommand(String input, Socket socket, InMemoryClientsRepository repository, Queue<ResponseFuture> responses)
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

            final User user = parseUser(input);

            final User result = repository.removeFilesByUsername(user.name(), user.filePaths());
            future.response().complete(ResponseFactory.createSuccess(result, socket.getChannel()));
        }
        catch (EntityNotFoundException e)
        {
            System.out.println("Server error during UnregisterClient command [" + e.getMessage() + "]");
            future.response().complete(ResponseFactory.createServerError(e, socket.getChannel()));
        }
        catch (ClientException e)
        {
            System.out.println("Client error during UnregisterClient command [" + e.getMessage() + "]");
            future.response().complete(ResponseFactory.createClientError(e, socket.getChannel()));
        }
    }


    private User parseUser(String input) throws ClientException
    {
        try
        {
            final String[] tokens = input.split(" ");

            if (tokens.length < MIN_COMMAND_ARGUMENTS)
            {
                throw new Exception("Bad command syntax [" + input + "]");
            }

            if (!tokens[0].equalsIgnoreCase("unregister"))
            {
                throw new Exception("Invalid command [" + tokens[0] + "]");
            }

            final String username = tokens[1];
            final String[] filePaths = Arrays.stream(tokens).skip(2).toArray(String[]::new);

            return new User(username, socket.getRemoteSocketAddress().toString(), Set.of(filePaths));
        }
        catch (Exception e)
        {
            throw new ClientException("Error while parsing", e, null, socket);
        }
    }
}
