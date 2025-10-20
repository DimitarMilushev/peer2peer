package main.java.d.milushev.models.commands;


import main.java.d.milushev.models.protocol.Response;
import main.java.d.milushev.models.protocol.ResponseFuture;
import main.java.d.milushev.repository.InMemoryClientsRepository;
import main.java.d.milushev.repository.models.User;

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
    private final Queue<Exception> errors;
    private final Queue<ResponseFuture> responses;


    public UnregisterCommand(String input,
                             Socket socket,
                             InMemoryClientsRepository repository,
                             Queue<Exception> errors,
                             Queue<ResponseFuture> responses)
    {
        this.input = input;
        this.socket = socket;
        this.repository = repository;
        this.errors = errors;
        this.responses = responses;
    }


    @Override
    public void run()
    {
        try
        {
            final ResponseFuture future = new ResponseFuture(socket.getChannel(), new CompletableFuture<>());
            responses.add(future);

            final User user = parseUser(input);

            final User result = repository.removeFilesByUsername(user.name(), user.filePaths());
            future.response().complete(new Response(result, 201, socket.getChannel()));
        }
        catch (Exception e)
        {
            System.out.println("Error during UnregisterClient command [" + e.getMessage() + "]");
            e.printStackTrace();
            this.errors.add(e);
        }
    }


    private User parseUser(String input) throws Exception
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
}
