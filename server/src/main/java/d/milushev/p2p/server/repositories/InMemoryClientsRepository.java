package main.java.d.milushev.p2p.server.repositories;


import main.java.d.milushev.p2p.server.exceptions.repository.EntityAlreadyExistsException;
import main.java.d.milushev.p2p.server.exceptions.repository.EntityNotFoundException;
import main.java.d.milushev.p2p.server.repositories.models.User;

import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class InMemoryClientsRepository
{
    private static final Logger LOG = LogManager.getLogger(InMemoryClientsRepository.class);
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
        LOG.info("Adding new client [{}]", user.name());
        if (usersByName.containsKey(user.name()))
        {
            throw new EntityAlreadyExistsException("User already exists.");
        }

        usersByName.put(user.name(), user);
        LOG.info("Successfully added new client [{}]", user.name());

        return usersByName.get(user.name());
    }


    public User removeFilesByUsername(String username, Set<String> files) throws EntityNotFoundException
    {
        LOG.info("Removing files [{}] for user [{}]", files, username);
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
        LOG.info("Successfully removed [{}] for user [{}]", files, username);

        return usersByName.get(username);
    }


    public User addFilesByUsername(String username, Set<String> files) throws EntityNotFoundException, EntityAlreadyExistsException
    {
        LOG.info("Registering files [{}] for user [{}]", files, username);
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
        LOG.info("Successfully registered files [{}] for user [{}]", files, username);

        return usersByName.get(username);
    }


    public List<User> removeByAddress(String address) throws EntityNotFoundException
    {
        LOG.info("Removing usernames with address [{}]", address);
        List<User> users = usersByName.values().stream().filter(user -> user.address().equals(address)).toList();
        if (users.isEmpty())
        {
            throw new EntityNotFoundException("No such address [" + address + "]");
        }

        for (var user : users)
        {
            usersByName.remove(user.name());
        }

        LOG.info("Successfully removed users [{}]", users);
        return users;
    }


    public boolean exists(String username)
    {
        return usersByName.containsKey(username);
    }
}
