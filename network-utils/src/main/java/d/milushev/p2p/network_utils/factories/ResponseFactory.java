package d.milushev.p2p.network_utils.factories;


import d.milushev.p2p.network_utils.enums.ResponseCodes;
import d.milushev.p2p.network_utils.models.Response;

import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;


public class ResponseFactory
{
    public static Response createClientError(Executors error, SocketChannel channel)
    {
        return new Response(error, ResponseCodes.CLIENT_ERROR.getErrorCode(), channel);
    }


    public static Response createServerError(Exception error, SocketChannel channel)
    {
        return new Response(error, ResponseCodes.SERVER_ERROR.getErrorCode(), channel);
    }


    public static Response createSuccess(Object payload, SocketChannel channel)
    {
        return new Response(payload, ResponseCodes.OK.getErrorCode(), channel);
    }
}
