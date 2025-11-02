package main.java.d.milushev.p2p.server.exceptions.database;

import java.io.Serial;

public class EntityNotFoundException extends DatabaseException
{
    @Serial
    private static final long serialVersionUID = 5884196327006156970L;


    public EntityNotFoundException(String message)
    {
        super(message);
    }
}
