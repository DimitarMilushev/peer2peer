package main.java.d.milushev.p2p.server.models.commands;


import d.milushev.p2p.network_utils.factories.ResponseFactory;
import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.ResponseFuture;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;


public class SlowHelloCommand implements Command
{
    private final Queue<ResponseFuture> queue;
    private final Request request;


    public SlowHelloCommand(Queue<ResponseFuture> responses, Request request)
    {
        this.queue = responses;
        this.request = request;
    }


    @Override
    public void run()
    {
        final var future = new ResponseFuture(request.channel(), new CompletableFuture<>());
        this.queue.offer(future);

        try
        {
            Thread.sleep(1000 * 10); // Fake slowdown

            final String msg = "Hello, [" + request.channel().getRemoteAddress() + "]! Data: [" + request.payload() + "]";

            future.response().complete(ResponseFactory.createSuccess(msg, request.channel()));
        }
        catch (InterruptedException | IOException e)
        {
            future.response().complete(ResponseFactory.createServerError(e, request.channel()));
        }
    }
}
