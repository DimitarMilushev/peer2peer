package main.java.d.milushev.p2p.client.db.exceptions;


public class TableCreationException extends DatabaseException
{
    public TableCreationException(String message)
    {
        super(message);
    }


    public TableCreationException(String message, Throwable e)
    {
        super(message, e);
    }
}
