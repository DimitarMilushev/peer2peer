package main.java.d.milushev.p2p.server.exceptions;


import java.net.Socket;


/**
 * Exception that is caused by the client input.
 */
public class ClientException extends Exception
{
    private final String username;
    private final Socket socket;


    public ClientException(String message, Throwable t, String username, Socket socket)
    {
        super(message, t);
        this.username = username;
        this.socket = socket;
    }


    @Override
    public String toString()
    {
        return "[user: " + username + ", address: " + socket.toString() + ", error: " + super.toString() + "]";
    }


    public String getUsername()
    {
        return username;
    }


    public Socket getSocket()
    {
        return socket;
    }
}
