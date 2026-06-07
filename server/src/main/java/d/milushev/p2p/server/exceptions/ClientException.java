package main.java.d.milushev.p2p.server.exceptions;


/**
 * Exception that is caused by the client input.
 */
public class ClientException extends Exception
{
    private final String username;
    private final String address;


    public ClientException(String message, Throwable t, String username, String address)
    {
        super(message, t);
        this.username = username;
        this.address = address;
    }


    @Override
    public String toString()
    {
        return "[user: " + username + ", address: " + address + ", error: " + getMessage() + ", cause: " + getCause() + "]";
    }

}
