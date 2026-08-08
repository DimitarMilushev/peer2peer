package main.java.d.milushev.p2p.server.exceptions.processor;


public class BadSyntaxException extends ProcessorException
{
    public BadSyntaxException(String message)
    {
        super("Bad syntax [" + message + "]");
    }
}
