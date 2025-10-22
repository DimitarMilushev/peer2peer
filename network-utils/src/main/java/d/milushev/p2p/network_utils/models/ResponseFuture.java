package d.milushev.p2p.network_utils.models;


import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;


public record ResponseFuture(SocketChannel channel, CompletableFuture<Response> response)
{
};
