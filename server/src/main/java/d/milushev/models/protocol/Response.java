package main.java.d.milushev.models.protocol;


import java.io.Serializable;
import java.nio.channels.SocketChannel;


public record Response(Object payload, int status, SocketChannel channel) implements Serializable
{
}
