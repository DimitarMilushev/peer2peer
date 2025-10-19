package main.java.d.milushev.repository.models;


import java.util.Set;


public record ClientMetadata(String name, String address, String port, Set<String> files)
{
}
