package main.java.d.milushev.p2p.server.exceptions.processor;


public class UnsupportedInputException extends ProcessorException
{
    public UnsupportedInputException(String input)
    {
        super("Operation [" + input + "] is not supported");
    }
}
