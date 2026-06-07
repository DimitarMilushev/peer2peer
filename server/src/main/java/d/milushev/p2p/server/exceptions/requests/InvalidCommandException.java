package main.java.d.milushev.p2p.server.exceptions.requests;


import main.java.d.milushev.p2p.server.exceptions.ClientException;

import java.net.Socket;


public class InvalidCommandException extends ClientException
{
    public InvalidCommandException(String message, Throwable t, String username, Socket address)
    {
        super(message, t, username, address);
    }
}
