package d.milushev.p2p.network_utils.models;


import java.io.Serial;
import java.io.Serializable;
import java.nio.channels.SocketChannel;


public record Response(Object payload, int status, SocketChannel channel) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 6770926572603869465L;
}
