package main.java.d.milushev.p2p.client;


import main.java.d.milushev.p2p.client.db.BasicDB;
import main.java.d.milushev.p2p.client.db.exceptions.TableCreationException;
import main.java.d.milushev.p2p.client.db.exceptions.TableModifyException;
import main.java.d.milushev.p2p.client.env.EnvProperties;
import main.java.d.milushev.p2p.client.filetransfer.v1.FileServer;
import main.java.d.milushev.p2p.client.metadata.MetadataUpdater;
import main.java.d.milushev.p2p.client.repository.ActiveUsersRepository;
import main.java.d.milushev.p2p.client.repository.RegisteredFilesRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class Main
{
    private static final Logger LOG = LogManager.getLogger(Main.class);


    public static void main(String[] args)
    {
        LOG.info("Starting P2P Client...");
        BasicDB db = new BasicDB("C:\\Users\\d.milushev\\repos\\personal\\peer2peer\\client\\src");
        try
        {
            db.createTable("test", Set.of("name", "age"));
            db.addRecords("test", Gosho.class, Set.of(new Gosho("test-name", 20)));
        }
        catch (TableCreationException e)
        {
            throw new RuntimeException(e);
        }
        catch (TableModifyException e)
        {
            throw new RuntimeException(e);
        }
        //        basicStart();
        LOG.info("P2P Client has been stopped.");
    }

    public static record Gosho(String name, int age) {}

    private static void basicStart()
    {
        final AtomicBoolean stopSignal = new AtomicBoolean(false);
        final ActiveUsersRepository usersRepository = new ActiveUsersRepository();
        final RegisteredFilesRepository filesRepository = new RegisteredFilesRepository();

        try (final var executor = Executors.newFixedThreadPool(3);
             final var console = new ConsoleInputListener(stopSignal, usersRepository, filesRepository);
             final var fileServer = new FileServer(filesRepository, EnvProperties.FILE_SERVER_PORT.getOrDefault());
        )
        {
            executor.submit(console);
            executor.submit(fileServer);
            executor.submit(new MetadataUpdater(usersRepository));

            LOG.info("Started P2P Client...");
            while (!stopSignal.get())
            {
                Thread.sleep(1000);
            }

            LOG.info("Stopping P2P Client...");
        }
        catch (InterruptedException e)
        {
            LOG.error("P2P Client interrupted", e);
            throw new RuntimeException(e);
        }
    }
}
