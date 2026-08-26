package main.java.d.milushev.p2p.client.db.exceptions;


public class InternalDatabaseException extends RuntimeException
{
    public InternalDatabaseException(String message)
    {
        super(message);
    }


    public InternalDatabaseException(String message, Throwable e)
    {
        super(message);
    }
}
