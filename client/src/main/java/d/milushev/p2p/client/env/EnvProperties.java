package main.java.d.milushev.p2p.client.env;


import d.milushev.p2p.env_utils.data.EnvInteger;
import d.milushev.p2p.env_utils.data.EnvString;


public class EnvProperties
{
    private EnvProperties()
    {
        // Hide constructor
    }


    public static final EnvString SERVER_HOST = new EnvString(EnvConstants.CENTRAL_SERVER_HOST, "localhost");
    public static final EnvInteger SERVER_PORT = new EnvInteger(EnvConstants.CENTRAL_SERVER_PORT, 8000);

    public static final EnvInteger FILE_SERVER_PORT = new EnvInteger(EnvConstants.FILE_SERVER_PORT, 8021);

    public static final EnvInteger UPDATER_TIMEOUT_S = new EnvInteger(EnvConstants.UPDATER_TIMEOUT_S, 10);
}
