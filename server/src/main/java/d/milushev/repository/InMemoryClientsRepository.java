package main.java.d.milushev.repository;


import main.java.d.milushev.repository.models.User;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class InMemoryClientsRepository
{
    private final ConcurrentMap<String, User> usersByName;


    public InMemoryClientsRepository()
    {
        this.usersByName = new ConcurrentHashMap<>();
    }


    public User[] getAllUsers()
    {
        return usersByName.values().toArray(User[]::new);
    }


    public User addUser(User user) throws Exception
    {
        System.out.println("Adding new client [" + user.name() + "]");
        if (usersByName.containsKey(user.name()))
        {
            throw new Exception("User already exists.");
        }

        usersByName.put(user.name(), user);
        System.out.println("Successfully added new client [" + user.name() + "]");

        return usersByName.get(user.name());
    }


    public User removeFilesByUsername(String username, Set<String> files) throws Exception
    {
        System.out.println("Removing files [" + files + " for user [" + username + "]");
        final User user = usersByName.get(username);
        if (user == null)
        {
            throw new Exception("User [" + username + "] doesn't exist");
        }

        for (var file : files)
        {
            if (!user.filePaths().contains(file))
            {
                throw new Exception("No such file [" + file + "] registered by user [" + username + "]");
            }
        }

        user.filePaths().removeAll(files);
        System.out.println("Successfully removed [" + files + "] for user [" + username + "]");

        return usersByName.get(username);
    }


    public User registerFilesByUsername(String username, Set<String> files) throws Exception
    {
        System.out.println("Registering files [" + files + "] for user [" + username + "]");
        final User user = usersByName.get(username);
        if (user == null)
        {
            throw new Exception("User [" + username + "] doesn't exist");
        }

        for (var file : files)
        {
            if (user.filePaths().contains(file))
            {
                throw new Exception("User [" + username + "] has already registered file [" + file + "]");
            }
        }

        user.filePaths().addAll(files);
        System.out.println("Successfully registered files [" + files + "] for user [" + username + "]");

        return usersByName.get(username);
    }


    public boolean exists(String username)
    {
        return usersByName.containsKey(username);
    }
}
