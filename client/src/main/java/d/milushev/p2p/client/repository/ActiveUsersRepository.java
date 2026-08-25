package main.java.d.milushev.p2p.client.repository;


import main.java.d.milushev.p2p.client.repository.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ActiveUsersRepository
{
    final Map<String, Set<String>> usernamesByAddress;


    public ActiveUsersRepository()
    {
        usernamesByAddress = new HashMap<>();
    }


    public void add(User user)
    {
        final Set<String> usernames = this.usernamesByAddress.computeIfAbsent(user.address(), k -> new java.util.HashSet<>());

        usernames.add(user.username());
    }


    public void addAll(List<User> users)
    {
        for (User user : users)
        {
            add(user);
        }
    }


    public User getByUsername(String username)
    {
        for (Map.Entry<String, Set<String>> entry : usernamesByAddress.entrySet())
        {
            if (entry.getValue().contains(username))
            {
                return new User(username, entry.getKey());
            }
        }

        return null;
    }


    public void drop()
    {
        usernamesByAddress.clear();
    }
}
