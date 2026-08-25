package d.milushev.p2p.envutil.data;


public class EnvString extends EnvData<String>
{
    protected EnvString(String envName, String defaultValue)
    {
        super(envName, defaultValue);
    }


    @Override
    protected String parseValue(String value)
    {
        return value;
    }
}
