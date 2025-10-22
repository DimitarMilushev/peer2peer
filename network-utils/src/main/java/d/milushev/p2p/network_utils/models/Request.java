package d.milushev.p2p.network_utils.models;


import java.io.Serial;
import java.io.Serializable;
import java.nio.channels.SocketChannel;


public record Request(Object payload, SocketChannel channel) implements Serializable
{
    @Serial
    private static final long serialVersionUID = -7445690639976478845L;
}
