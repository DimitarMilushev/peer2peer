package main.java.d.milushev.p2p.client.repository;


import main.java.d.milushev.p2p.client.repository.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ActiveUsersRepository
{
    final Map<String, Set<String>> addressByUsername;


    public ActiveUsersRepository()
    {
        addressByUsername = new HashMap<>();
    }


    public void add(User user)
    {
        final Set<String> usernamesByAddress = addressByUsername.computeIfAbsent(user.address(), k -> new java.util.HashSet<>());

        usernamesByAddress.add(user.username());
    }


    public void addAll(List<User> users)
    {
        for (User user : users)
        {
            add(user);
        }
    }
}
