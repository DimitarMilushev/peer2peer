package main.java.d.milushev.p2p.server.commands;


import d.milushev.p2p.network_utils.factories.ResponseFactory;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.exceptions.ClientException;
import main.java.d.milushev.p2p.server.exceptions.database.EntityAlreadyExistsException;
import main.java.d.milushev.p2p.server.repository.InMemoryClientsRepository;
import main.java.d.milushev.p2p.server.repository.models.User;

import java.net.Socket;
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

            final User result = registerFiles(parseUser(input, socket.getRemoteSocketAddress().toString()));
            future.response().complete(ResponseFactory.createSuccess(result, socket.getChannel()));
        }
        catch (EntityAlreadyExistsException e)
        {
            System.out.println("Server error during RegisterClient command [" + e.getMessage() + "]");
            e.printStackTrace();

            future.response().complete(ResponseFactory.createServerError(e, socket.getChannel()));
        }
        catch (ClientException e)
        {
            System.out.println("Client error during RegisterClient command [" + e.getMessage() + "]");


            future.response().complete(ResponseFactory.createClientError(e, socket.getChannel()));
        }
    }


    private User registerFiles(User user) throws EntityAlreadyExistsException
    {
        if (repository.exists(user.name()))
        {
            return repository.addFilesByUsername(user.name(), user.filePaths());
        }

        return repository.addUser(user);
    }


    private User parseUser(String input, String address) throws ClientException
    {
        try
        {
            final String[] tokens = input.split(" ");

            if (tokens.length < MIN_COMMAND_ARGUMENTS)
            {
                throw new Exception("Bad command syntax [" + input + "]");
            }

            if (!tokens[0].equalsIgnoreCase("register"))
            {
                throw new Exception("Invalid command [" + tokens[0] + "]");
            }

            final String username = tokens[1];
            final String[] filePaths = Arrays.stream(tokens).skip(2).toArray(String[]::new);
            return new User(username, address, new HashSet<>(List.of(filePaths)));
        }
        catch (Exception e)
        {
            throw new ClientException(e.getMessage(), e, null, socket);
        }
    }
}
