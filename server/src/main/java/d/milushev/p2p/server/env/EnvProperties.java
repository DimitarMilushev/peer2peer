package main.java.d.milushev.p2p.server.env;


import d.milushev.p2p.env_utils.data.EnvInteger;


public class EnvProperties
{
    private EnvProperties()
    {
        // Hide constructor
    }


    public static final EnvInteger SERVER_PORT = new EnvInteger(EnvConstants.CENTRAL_SERVER_PORT, 8000);
}
