package main.java.d.milushev.p2p.client.db.exceptions;


public class TableDeletionException extends DatabaseException
{
    public TableDeletionException(String message)
    {
        super(message);
    }


    public TableDeletionException(String message, Throwable e)
    {
        super(message, e);
    }
}
