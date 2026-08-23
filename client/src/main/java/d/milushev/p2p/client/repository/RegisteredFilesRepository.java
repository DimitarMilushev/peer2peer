package main.java.d.milushev.p2p.client.repository;


import java.util.HashMap;
import java.util.Map;


public class RegisteredFilesRepository
{
    private final Map<String, String> filePathByName;


    public RegisteredFilesRepository()
    {
        this.filePathByName = new HashMap<>();
    }


    public void addFile(String fileName, String filePath)
    {
        this.filePathByName.put(fileName, filePath);
    }


    public boolean hasFile(String fileName)
    {
        return this.filePathByName.containsKey(fileName);
    }


    public String getFilePath(String fileName)
    {
        return this.filePathByName.get(fileName);
    }


    public void removeFile(String fileName)
    {
        this.filePathByName.remove(fileName);
    }
}
