package main.java.d.milushev.p2p.server.repository;


import main.java.d.milushev.p2p.server.exceptions.database.DatabaseException;
import main.java.d.milushev.p2p.server.exceptions.database.EntityAlreadyExistsException;
import main.java.d.milushev.p2p.server.exceptions.database.EntityNotFoundException;
import main.java.d.milushev.p2p.server.repository.models.User;

import java.util.List;
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


    public User addUser(User user) throws EntityAlreadyExistsException
    {
        System.out.println("Adding new client [" + user.name() + "]");
        if (usersByName.containsKey(user.name()))
        {
            throw new EntityAlreadyExistsException("User already exists.");
        }

        usersByName.put(user.name(), user);
        System.out.println("Successfully added new client [" + user.name() + "]");

        return usersByName.get(user.name());
    }


    public User removeFilesByUsername(String username, Set<String> files) throws EntityNotFoundException
    {
        System.out.println("Removing files [" + files + " for user [" + username + "]");
        final User user = usersByName.get(username);
        if (user == null)
        {
            throw new EntityNotFoundException("User [" + username + "] doesn't exist");
        }

        for (var file : files)
        {
            if (!user.filePaths().contains(file))
            {
                throw new EntityNotFoundException("No such file [" + file + "] registered by user [" + username + "]");
            }
        }

        user.filePaths().removeAll(files);
        System.out.println("Successfully removed [" + files + "] for user [" + username + "]");

        return usersByName.get(username);
    }


    public User addFilesByUsername(String username, Set<String> files) throws EntityNotFoundException, EntityAlreadyExistsException
    {
        System.out.println("Registering files [" + files + "] for user [" + username + "]");
        final User user = usersByName.get(username);
        if (user == null)
        {
            throw new EntityNotFoundException("User [" + username + "] doesn't exist");
        }

        for (var file : files)
        {
            if (user.filePaths().contains(file))
            {
                throw new EntityAlreadyExistsException("User [" + username + "] has already registered file [" + file + "]");
            }
        }

        user.filePaths().addAll(files);
        System.out.println("Successfully registered files [" + files + "] for user [" + username + "]");

        return usersByName.get(username);
    }


    public List<User> removeByAddress(String address) throws EntityNotFoundException
    {
        System.out.println("Removing usernames with address [" + address + "]");
        List<User> users = usersByName.values().stream().filter(user -> user.address().equals(address)).toList();
        if (users.isEmpty())
        {
            throw new EntityNotFoundException("No such address [" + address + "]");
        }

        for (var user : users)
        {
            usersByName.remove(user.name());
        }

        System.out.println("Successfully removed users [" + users + "]");
        return users;
    }


    public boolean exists(String username)
    {
        return usersByName.containsKey(username);
    }
}
