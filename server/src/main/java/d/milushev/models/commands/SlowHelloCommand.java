package main.java.d.milushev.models.commands;


import main.java.d.milushev.models.protocol.Request;
import main.java.d.milushev.models.protocol.Response;
import main.java.d.milushev.models.protocol.ResponseFuture;

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
            Thread.sleep(1000 * 10);
            final String msg = "Hello, [" + request.channel()
                                                   .getRemoteAddress() + "]! Data: [" + request.path() + ", " + request.payload() + "]";

            future.response().complete(new Response(msg, 200, request.channel()));
        }
        catch (InterruptedException | IOException e)
        {
            future.response().complete(new Response(e.getMessage(), 500, request.channel()));
        }
    }
}
