package main.java.d.milushev.models.protocol;


import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;


public record ResponseFuture(SocketChannel channel, CompletableFuture<Response> response)
{
}
