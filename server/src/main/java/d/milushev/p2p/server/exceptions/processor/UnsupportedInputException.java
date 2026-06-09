package main.java.d.milushev.p2p.server.exceptions.processor;


public class UnsupportedCommandException extends ProcessorException
{
    public UnsupportedCommandException(String input)
    {
        super("Command " + input + " is not supported");
    }
}
