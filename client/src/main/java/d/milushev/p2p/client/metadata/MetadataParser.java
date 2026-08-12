package main.java.d.milushev.p2p.client.metadata;


import main.java.d.milushev.p2p.client.repository.models.User;

import java.util.ArrayList;
import java.util.List;


public class MetadataParser
{
    private static final String USER_SEPARATOR = ";";
    private static final String PROP_SEPARATOR = ", ";
    private static final String NAME_PROP = "name";
    private static final String ADDRESS_PROP = "address";


    public static List<User> parseUsers(String metadata)
    {
        if (metadata == null || metadata.isBlank())
        {
            return List.of();
        }

        final String[] userMetadataArray = metadata.split(USER_SEPARATOR);
        final List<User> users = new ArrayList<>();
        for (String userMetadata : userMetadataArray)
        {
            final User user = parseUser(userMetadata);
            if (user == null)
            {
                continue;
            }

            users.add(user);
        }

        return users;
    }


    public static User parseUser(String userInfo)
    {
        if (userInfo == null || userInfo.isBlank())
        {
            return null;
        }

        final String[] props = userInfo.split(PROP_SEPARATOR);

        if (props.length != 2)
        {
            return null;
        }

        final String name = props[0].replace(NAME_PROP + "=", "").trim();
        final String address = props[1].replace(ADDRESS_PROP + "=", "").trim();

        return new User(name, address);
    }
}
