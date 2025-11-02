package main.java.d.milushev.p2p.server.exceptions.database;

import java.io.Serial;
import java.io.Serializable;

public class DatabaseException extends RuntimeException implements Serializable
{
    @Serial
    private static final long serialVersionUID = -6887233529068524661L;


    public DatabaseException(String message)
    {
        super(message);
    }
}
