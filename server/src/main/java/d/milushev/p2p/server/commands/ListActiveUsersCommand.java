package main.java.d.milushev.p2p.server.commands;


import d.milushev.p2p.network_utils.factories.ResponseFactory;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;
import main.java.d.milushev.p2p.server.repositories.models.User;

import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class ListActiveUsersCommand implements Command
{
    public static final String NAME = "list-active-users";

    private final Socket socket;
    private final InMemoryClientsRepository repository;
    private final Consumer<ResponseFuture> onResponse;


    public ListActiveUsersCommand(Socket socket, InMemoryClientsRepository repository, Consumer<ResponseFuture> onResponse)
    {
        this.socket = socket;
        this.repository = repository;
        this.onResponse = onResponse;
    }


    public void run()
    {
        final ResponseFuture future = new ResponseFuture(socket.getChannel(), new CompletableFuture<>());
        onResponse.accept(future);

        final User[] users = repository.getAllUsers();
        if (users.length == 0)
        {
            future.response().complete(ResponseFactory.createSuccess("[]", socket.getChannel()));
            return;
        }

        final String[] userInfos = Arrays.stream(users).map(this::getUserInfo).toArray(String[]::new);

        final String result = String.join(";", userInfos);

         future.response().complete(ResponseFactory.createSuccess(result, socket.getChannel()));
    }


    private String getUserInfo(User user)
    {
        return "name=" + user.name() + ", address=" + user.address();
    }
}
