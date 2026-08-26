package main.java.d.milushev.p2p.client.db.exceptions;


public class DatabaseException extends Exception
{
    public DatabaseException(String message)
    {
        super(message);
    }


    public DatabaseException(String message, Throwable e)
    {
        super(message, e);
    }
}
