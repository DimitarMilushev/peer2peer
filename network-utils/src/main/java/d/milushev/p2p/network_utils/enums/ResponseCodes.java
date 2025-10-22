package d.milushev.p2p.network_utils.enums;


public enum ResponseCodes
{
    SERVER_ERROR(500), CLIENT_ERROR(400), OK(200);

    private final int errorCode;


    public int getErrorCode()
    {
        return this.errorCode;
    }


    ResponseCodes(int errorCode)
    {
        this.errorCode = errorCode;
    }
}
