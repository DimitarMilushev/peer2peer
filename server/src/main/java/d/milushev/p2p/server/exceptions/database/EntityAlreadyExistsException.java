package main.java.d.milushev.p2p.server.exceptions.database;

import java.io.Serial;

public class EntityAlreadyExistsException extends DatabaseException
{
    @Serial
    private static final long serialVersionUID = -498069017217282714L;


    public EntityAlreadyExistsException(String message)
    {
        super(message);
    }
}
