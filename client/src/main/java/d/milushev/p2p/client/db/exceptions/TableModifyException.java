package main.java.d.milushev.p2p.client.db.exceptions;


public class TableModifyException extends DatabaseException
{
    public TableModifyException(String message)
    {
        super(message);
    }


    public TableModifyException(String message, Throwable e)
    {
        super(message, e);
    }
}
