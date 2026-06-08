package main.java.d.milushev.p2p.server.listener;


import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;
import main.java.d.milushev.p2p.server.utils.BufferUtils;
import main.java.d.milushev.p2p.server.exceptions.InvalidConnectionHandling;
import main.java.d.milushev.p2p.server.exceptions.ServerException;
import main.java.d.milushev.p2p.server.commands.CloseUserCommand;
import main.java.d.milushev.p2p.server.commands.ListFilesCommand;
import main.java.d.milushev.p2p.server.commands.RegisterCommand;
import main.java.d.milushev.p2p.server.commands.SlowHelloCommand;
import main.java.d.milushev.p2p.server.commands.UnregisterCommand;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ConnectionHandler
{

    private static final Logger LOG = LogManager.getLogger(ConnectionHandler.class);
    //    private final Queue<Request> requests = new LinkedList<>();
    private final Queue<ResponseFuture> responses;
    private final ActiveConnections connections;
    private final ExecutorService executor;
    private final InMemoryClientsRepository repository;


    public ConnectionHandler(ActiveConnections connections, ExecutorService executor)
    {
        this.connections = connections;
        this.executor = executor;

        repository = new InMemoryClientsRepository();
        responses = new LinkedList<>();
    }


    public void handleWrite(SelectionKey key) throws ServerException
    {
        try
        {
            if (key.channel() instanceof SocketChannel clientChannel)
            {
                final Socket socket = clientChannel.socket();

                if (responses.isEmpty() || responses.peek().channel() != clientChannel)
                {
                    return;
                }

                LOG.info("Handling WRITE for [{}]", socket.getRemoteSocketAddress());
                if (!responses.peek().response().isDone())
                {
                    return;
                }

                final var buffer = this.connections.getBuffer(socket);
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
                LOG.info("Successfully handled WRITE for [{}]: {}", socket.getRemoteSocketAddress(), response);
                return;
            }

            throw new InvalidConnectionHandling("Invalid channel was opened for WRITE operation - " + key.channel());
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to process WRITE operation", e);
        }
    }


    public void handleRead(SelectionKey key) throws ServerException
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            final var socket = clientChannel.socket();
            LOG.info("Handling read for [{}]", socket.getRemoteSocketAddress());

            try
            {
                final var buffer = this.connections.getBuffer(socket);
                buffer.clear();

                int bytesRead = clientChannel.read(buffer);
                final StringBuilder sb = new StringBuilder();
                while (bytesRead > 0)
                {
                    buffer.flip();
                    sb.append(BufferUtils.bufferToString(buffer, bytesRead));

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
                LOG.info("READ finished for [{}]: {}", socket.getRemoteSocketAddress(), sb);
            }
            catch (Exception e)
            {
                LOG.error("Connection issue. Closing channel [{}]", socket.getRemoteSocketAddress(), e);
                closeClientChannel(socket);

                throw new ServerException("Failed to process READ operation", e);
            }

            return;
        }

        throw new ServerException("Failed to process WRITE operation",
                                  new InvalidConnectionHandling("Invalid channel was opened for READ operation"));
    }


    private void closeClientChannel(Socket socket) throws ServerException
    {
        LOG.info("Closing client channel [{}]", socket.getRemoteSocketAddress());

        try
        {
            new CloseUserCommand(socket, repository).run();

            connections.remove(socket);
            socket.close();
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
            if (key.channel() instanceof ServerSocketChannel serverChannel)
            {
                LOG.info("Handling ACCEPT");
                final var channel = serverChannel.accept();
                final var socket = channel.socket();

                channel.configureBlocking(false);
                channel.register(key.selector(), SelectionKey.OP_READ);
                key.attach(ByteBuffer.allocate(1024));

                connections.add(socket);
                LOG.info("Successfully accepted client channel [{}]", socket.getRemoteSocketAddress());
                return;
            }

            throw new InvalidConnectionHandling("Invalid channel was opened for ACCEPT operation");
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to process ACCEPT operation", e);
        }
    }

}
