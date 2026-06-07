package main.java.d.milushev.p2p.server.exceptions;


/**
 * Exception that is caused by the server business logic.
 */
public class ServerException extends Exception
{
    public ServerException(String message, Throwable t)
    {
        super(message, t);
    }

    public ServerException(String message)
    {
        super(message);
    }
}
