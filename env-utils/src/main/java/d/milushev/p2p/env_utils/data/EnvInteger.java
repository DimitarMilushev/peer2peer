package d.milushev.p2p.env_utils.data;


public class EnvInteger extends EnvData<Integer>
{
    public EnvInteger(String envName, Integer defaultValue)
    {
        super(envName, defaultValue);
    }


    @Override
    protected Integer parseValue(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
