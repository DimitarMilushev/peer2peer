package d.milushev.p2p.env_utils.data;


public abstract class EnvData<T>
{
    private final String envName;
    private final T defaultValue;


    protected EnvData(String envName, T defaultValue)
    {
        this.envName = envName;
        this.defaultValue = defaultValue;
    }


    public T getDefaultValue()
    {
        return defaultValue;
    }


    public T getValue()
    {
        final String stringValue = System.getenv(envName);
        if (stringValue == null)
        {
            return null;
        }

        return parseValue(stringValue);
    }


    public T getOrDefault()
    {
        final T value = getValue();
        if (value == null)
        {
            return getDefaultValue();
        }

        return value;
    }


    /**
     * Parses the {@link String} value into {@link T}.
     */
    protected abstract T parseValue(String value);
}
