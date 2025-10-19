package main.java.d.milushev.exceptions;


public class InvalidConnectionHandling extends ServerException
{
    public InvalidConnectionHandling(String message, Throwable t)
    {
        super(message, t);
    }

    public InvalidConnectionHandling(String message)
    {
        super(message);
    }
}
