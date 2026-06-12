package main.java.d.milushev.p2p.server.commands;


import d.milushev.p2p.network_utils.factories.ResponseFactory;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;
import main.java.d.milushev.p2p.server.repositories.models.User;

import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class ListFilesCommand implements Command
{
    public static final String NAME = "list-files";

    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Consumer<ResponseFuture> onResponse;


    public ListFilesCommand(Socket socket, InMemoryClientsRepository repository, Consumer<ResponseFuture> onResponse)
    {
        this.socket = socket;
        this.repository = repository;
        this.onResponse = onResponse;
    }


    @Override
    public void run()
    {
        final ResponseFuture future = new ResponseFuture(socket.getChannel(), new CompletableFuture<>());
        onResponse.accept(future);

        final User[] users = repository.getAllUsers();
        final String[] filePerUser = Arrays.stream(users).map(this::getFilesByUser).toArray(String[]::new);

        final String result = Arrays.toString(filePerUser);
        future.response().complete(ResponseFactory.createSuccess(result, socket.getChannel()));
    }


    private String getFilesByUser(User user)
    {
        if (user.filePaths().isEmpty())
        {
            return user.name() + " : []";
        }

        return user.name() + " : [" + String.join(", ", user.filePaths()) + "]";
    }
}
