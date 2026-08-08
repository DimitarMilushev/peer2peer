package main.java.d.milushev.p2p.server.listener;


import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.Response;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A class that handles inbound and outbound messages.
 */
public class MessageMediator
{
    private static final Logger LOG = LogManager.getLogger(MessageMediator.class);

    private final BlockingQueue<Request> requests;
    private final BlockingQueue<ResponseFuture> responses;


    public MessageMediator()
    {
        requests = new LinkedBlockingQueue<>();
        responses = new LinkedBlockingQueue<>();
    }


    public void respond(ResponseFuture response)
    {
        LOG.info("Enqueuing response [{}]", response);
        this.responses.add(response);
    }


    public void request(Request request)
                    throws IOException
    {
        LOG.info("Enqueuing request [{}] from channel [{}]", request.payload(), request.channel().getRemoteAddress());
        this.requests.add(request);
    }


    public Request poll()
                    throws InterruptedException, IOException
    {
        final var request = requests.take();
        LOG.info("Polled request [{}] from channel [{}]", request.payload(), request.channel().getRemoteAddress());
        return request;
    }


    public CompletableFuture<Response> getResponseForChannel(SocketChannel channel)
                    throws IOException
    {
        LOG.debug("Getting response for channel [{}]", channel.getRemoteAddress());
        final var future = responses.peek();
        if (future == null || future.channel() != channel)
        {
            return null;
        }

        return responses.poll().response();
    }
}
