package d.milushev.p2p.env_utils.data;


public class EnvString extends EnvData<String>
{
    public EnvString(String envName, String defaultValue)
    {
        super(envName, defaultValue);
    }


    @Override
    protected String parseValue(String value)
    {
        return value;
    }
}
