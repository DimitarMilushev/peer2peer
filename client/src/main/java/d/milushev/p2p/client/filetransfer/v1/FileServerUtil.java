package main.java.d.milushev.p2p.client.filetransfer.v1;


import java.nio.file.Path;
import java.nio.file.Paths;


public class FileServerUtil
{
    private static final String UPLOADS_DIR = ".p2p/uploads";
    private static final String DOWNLOADS_DIR = ".p2p/downloads";


    private FileServerUtil()
    {
        // Private constructor to prevent instantiation
    }


    public static void prepareDirectories()
    {
        final Path uploadsDir = getUploadsDirectory();
        if (!uploadsDir.toFile().exists())
        {
            final boolean success = uploadsDir.toFile().mkdirs();
            if (!success)
            {
                System.out.println("Failed to create uploads directory: " + uploadsDir);
            }
        }

        final Path downloadsDir = getDownloadsDirectory();
        if (!downloadsDir.toFile().exists())
        {
            final boolean success = downloadsDir.toFile().mkdirs();
            if (!success)
            {
                System.out.println("Failed to create downloads directory: " + downloadsDir);
            }
        }
    }


    public static Path getUploadsDirectory()
    {
        return Paths.get(System.getProperty("user.home"), UPLOADS_DIR);
    }


    public static Path getDownloadsDirectory()
    {
        return Paths.get(System.getProperty("user.home"), DOWNLOADS_DIR);
    }
}
