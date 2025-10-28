package main.java.d.milushev.p2p.server.listener;


import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.BufferUtils;
import main.java.d.milushev.p2p.server.exceptions.InvalidConnectionHandling;
import main.java.d.milushev.p2p.server.exceptions.ServerException;
import main.java.d.milushev.p2p.server.models.commands.CloseUserCommand;
import main.java.d.milushev.p2p.server.models.commands.ListFilesCommand;
import main.java.d.milushev.p2p.server.models.commands.RegisterCommand;
import main.java.d.milushev.p2p.server.models.commands.SlowHelloCommand;
import main.java.d.milushev.p2p.server.models.commands.UnregisterCommand;
import main.java.d.milushev.p2p.server.repository.InMemoryClientsRepository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;


public class ConnectionHandler
{

    private final Queue<Request> requests = new LinkedList<>();
    private final Queue<ResponseFuture> responses = new LinkedList<>();
    private final ActiveConnections connections;
    private final ExecutorService executor;
    private final InMemoryClientsRepository repository;


    public ConnectionHandler(ActiveConnections connections, ExecutorService executor)
    {
        this.connections = connections;
        this.executor = executor;
        this.repository = new InMemoryClientsRepository();
    }


    public void handleWrite(SelectionKey key) throws ServerException
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


    public void handleRead(SelectionKey key) throws ServerException
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

                if (sb.toString().startsWith("register"))
                {
                    executor.execute(new RegisterCommand(sb.toString(), clientChannel.socket(), repository, responses));
                }
                else if (sb.toString().startsWith("unregister"))
                {
                    executor.execute(new UnregisterCommand(sb.toString(), clientChannel.socket(), repository, responses));
                }
                else if (sb.toString().startsWith("list-files"))
                {
                    executor.execute(new ListFilesCommand(clientChannel.socket(), repository, responses));
                }
                else
                {
                    executor.execute(new SlowHelloCommand(responses, new Request(sb.toString(), clientChannel)));
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

            new CloseUserCommand(channel.socket(), repository).run();
            connections.remove(channel);
            channel.close();
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to close channel", e);
        }
    }


    public void handleAccept(SelectionKey key) throws ServerException
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

}
