package main.java.d.milushev.p2p.server.repositories.models;


import java.util.Set;


public record User(String name, String address, Set<String> filePaths)
{
}
