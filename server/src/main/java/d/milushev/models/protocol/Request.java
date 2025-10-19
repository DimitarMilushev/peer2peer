package main.java.d.milushev.models.protocol;


import java.nio.channels.SocketChannel;


public record Request(String path, Object payload, SocketChannel channel)
{
}
