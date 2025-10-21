package main.java.d.milushev.server;


import main.java.d.milushev.ActiveConnections;
import main.java.d.milushev.BufferUtils;
import main.java.d.milushev.exceptions.InvalidConnectionHandling;
import main.java.d.milushev.exceptions.ServerException;
import main.java.d.milushev.models.commands.ListFilesCommand;
import main.java.d.milushev.models.commands.RegisterCommand;
import main.java.d.milushev.models.commands.SlowHelloCommand;
import main.java.d.milushev.models.commands.UnregisterCommand;
import main.java.d.milushev.models.commands.CloseUserCommand;
import main.java.d.milushev.models.protocol.Request;
import main.java.d.milushev.models.protocol.ResponseFuture;
import main.java.d.milushev.repository.InMemoryClientsRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Listener implements Runnable, AutoCloseable
{
    private static final Logger LOGGER = Logger.getGlobal();

    private final int port;

    private volatile boolean isActive;

    private final ServerSocketChannel serverChannel;
    private final Selector selector;

    private final Queue<Exception> errors;
    private final ActiveConnections connections;

    //TODO: Remove this. It's for echo server purposes
    private final Map<SocketChannel, String> lastMessageMap = new HashMap<>();
    //    private final DemoProcessor processor;
    private final Queue<Request> requests = new LinkedList<>();
    private final Queue<ResponseFuture> responses = new LinkedList<>();
    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
    private final InMemoryClientsRepository repository = new InMemoryClientsRepository();


    public Listener(int port) throws IOException
    {
        this.port = port;
        this.serverChannel = ServerSocketChannel.open();
        this.selector = Selector.open();
        this.errors = new LinkedList<>();
        this.connections = new ActiveConnections();
        isActive = false;

        //        this.processor = new DemoProcessor(requests, responses);
    }


    public void stop()
    {
        this.isActive = false;
    }


    public boolean isActive()
    {
        return this.isActive;
    }


    @Override
    public void run()
    {
        this.isActive = true;

        try
        {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Started..");
            Instant timeLastAccepted = Instant.now();
            boolean printTime = true;

            while (isActive)
            {
                final int readyChannels = selector.select(1000);

                if (!this.isActive || readyChannels == 0)
                {
                    continue;
                }

                for (var key : selector.selectedKeys())
                {
                    if (!key.isValid())
                    {
                        continue;
                    }

                    if (key.isAcceptable())
                    {
                        handleAccept(key);
                        timeLastAccepted = Instant.now();
                        printTime = true;
                    }
                    else if (key.isReadable())
                    {
                        handleRead(key);
                    }
                    else if (key.isWritable())
                    {
                        handleWrite(key);
                    }
                }

                if (printTime & responses.isEmpty())
                {
                    System.out.println("It took " + Duration.between(timeLastAccepted, Instant.now()).toSeconds() + " seconds.");
                    printTime = false;
                }

                selector.selectedKeys().clear();
            }
        }
        catch (IOException e)
        {
            System.out.println("Failed during server startup: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed during server startup: " + e.getMessage(), e);
        }
        catch (ServerException e)
        {
            System.out.println("An internal server error has occurred: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "An internal server error has occurred: " + e.getMessage(), e);
        }
        finally
        {
            System.out.println("Stopping server...");
            stop();
        }
    }


    private void handleWrite(SelectionKey key) throws ServerException
    {
        try
        {
            if (key.channel() instanceof SocketChannel clientChannel)
            {
                if (responses.isEmpty() || responses.peek().channel() != clientChannel)
                {
                    //                    System.out.println("No response found");
                    return;
                }

                System.out.println("Handling WRITE for [" + clientChannel.getRemoteAddress() + "]");
                if (!responses.peek().response().isDone())
                {
                    //                    System.out.println("Not yet ready...");
                    return;
                }

                final var buffer = this.connections.getBuffer(clientChannel);
                buffer.clear();

                final String response = responses.poll().response().get().toString();
                buffer.put(response.getBytes(StandardCharsets.UTF_8));

                buffer.flip();
                while (buffer.hasRemaining())
                {
                    clientChannel.write(buffer);
                }

                buffer.clear();
                key.interestOps(SelectionKey.OP_READ);
                System.out.println("Successfully handled WRITE for [" + clientChannel.getRemoteAddress() + "]: " + response);
                return;
            }

            throw new InvalidConnectionHandling("Invalid channel was opened for WRITE operation");
        }
        catch (InvalidConnectionHandling e)
        {
            throw new ServerException("Failed to process write operation", e);
        }
        catch (IOException e)
        {
            throw new ServerException("Failed to open connection", e);
        }
        catch (Exception e)
        {
            throw new ServerException("Something went wrong", e);
        }
    }


    private void handleRead(SelectionKey key) throws ServerException
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            final var clientSocket = clientChannel.socket();
            System.out.println("Handling read for [" + clientSocket.getRemoteSocketAddress() + "]");
            try
            {
                final var buffer = this.connections.getBuffer(clientChannel);
                buffer.clear();

                int bytesRead = clientChannel.read(buffer);
                final StringBuilder sb = new StringBuilder();
                while (bytesRead > 0)
                {
                    buffer.flip();
                    sb.append(BufferUtils.BufferToString(buffer, bytesRead));

                    buffer.clear();
                    bytesRead = clientChannel.read(buffer);
                }

                lastMessageMap.put(clientChannel, sb.toString());

                if (sb.toString().startsWith("register"))
                {
                    executor.execute(new RegisterCommand(sb.toString(), clientChannel.socket(), repository, errors, responses));
                }
                else if (sb.toString().startsWith("unregister"))
                {
                    executor.execute(new UnregisterCommand(sb.toString(), clientChannel.socket(), repository, errors, responses));
                }
                else if (sb.toString().startsWith("list-files"))
                {
                    executor.execute(new ListFilesCommand(clientChannel.socket(), repository, errors, responses));
                }
                else
                {
                    executor.execute(new SlowHelloCommand(responses, new Request(sb.toString(), null, clientChannel)));
                }

                buffer.clear();
                if (bytesRead == -1)
                {
                    throw new IOException("Connection closed.");
                }

                key.interestOps(SelectionKey.OP_WRITE);
                System.out.println("READ finished for [" + clientSocket.getRemoteSocketAddress() + "]: " + sb);
            }
            catch (Exception e)
            {
                System.out.println("Connection issue. Closing channel [" + clientSocket.getRemoteSocketAddress() + "]");
                closeClientChannel(clientChannel);
            }

            return;
        }

        throw new ServerException("Failed to process WRITE operation",
                                  new InvalidConnectionHandling("Invalid channel was opened for READ operation"));
    }


    private void closeClientChannel(SocketChannel channel) throws ServerException
    {
        try
        {

            System.out.println("Closing client channel " + channel.getRemoteAddress().toString());
            //            LOGGER.info("Closing client channel " + channel.getRemoteAddress().toString());

            new CloseUserCommand(channel.socket(), repository, errors).run();
            connections.remove(channel);
            channel.close();
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to close channel", e);
        }
    }


    private void handleAccept(SelectionKey key) throws ServerException
    {
        try
        {
            if (key.channel() instanceof ServerSocketChannel channel)
            {
                System.out.println("Handling ACCEPT");
                final var clientChannel = channel.accept();
                final var clientSocket = clientChannel.socket();

                clientChannel.configureBlocking(false);
                clientChannel.register(key.selector(), SelectionKey.OP_READ);
                key.attach(ByteBuffer.allocate(1024));

                connections.add(clientChannel);
                System.out.println("Successfully accepted client channel [" + clientSocket.getRemoteSocketAddress() + "]");
                return;
            }

            throw new InvalidConnectionHandling("Invalid channel was opened for ACCEPT operation");
        }
        catch (InvalidConnectionHandling e)
        {
            throw new ServerException("Failed to accept connection", e);
        }
        catch (IOException e)
        {
            throw new ServerException("Failed to open connection", e);
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to add connection", e);
        }
    }


    @Override
    public void close() throws IOException
    {
        System.out.println("Closing...");

        isActive = false;
        connections.closeAll();
        selector.close();
        serverChannel.close();
    }
}
