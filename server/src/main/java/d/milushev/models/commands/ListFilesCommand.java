package main.java.d.milushev.models.commands;


import main.java.d.milushev.models.protocol.Response;
import main.java.d.milushev.models.protocol.ResponseFuture;
import main.java.d.milushev.repository.InMemoryClientsRepository;
import main.java.d.milushev.repository.models.User;

import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class ListFilesCommand implements Command
{
    public static final String NAME = "list-files";
    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Queue<Exception> errors;
    private final Queue<ResponseFuture> responses;


    public ListFilesCommand(Socket socket, InMemoryClientsRepository repository, Queue<Exception> errors, Queue<ResponseFuture> responses)
    {
        this.socket = socket;
        this.repository = repository;
        this.errors = errors;
        this.responses = responses;
    }


    @Override
    public void run()
    {
        final ResponseFuture future = new ResponseFuture(socket.getChannel(), new CompletableFuture<>());
        responses.add(future);

        final User[] users = repository.getAllUsers();
        final String[] filePerUser = Arrays.stream(users).map(this::getFilesByUser).toArray(String[]::new);

        final String result = Arrays.toString(filePerUser);
        future.response().complete(new Response(result, 201, socket.getChannel()));
    }


    private String getFilesByUser(User user)
    {
        return user.filePaths().stream().map(x -> user.name() + " : " + x).collect(Collectors.joining(", "));
    }
}
