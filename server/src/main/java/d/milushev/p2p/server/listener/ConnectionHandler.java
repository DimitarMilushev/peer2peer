package main.java.d.milushev.p2p.server.listener;


import d.milushev.p2p.network_utils.SocketUtils;
import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.Response;
import main.java.d.milushev.p2p.server.exceptions.InvalidConnectionHandling;
import main.java.d.milushev.p2p.server.exceptions.ServerException;
import main.java.d.milushev.p2p.server.commands.CloseUserCommand;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ConnectionHandler
{

    private static final Logger LOG = LogManager.getLogger(ConnectionHandler.class);
    private final ActiveConnections connections;
    private final MessageMediator messageMediator;


    public ConnectionHandler(ActiveConnections connections, MessageMediator messageMediator)
    {
        this.connections = connections;
        this.messageMediator = messageMediator;
    }


    public void handleWrite(SelectionKey key)
                    throws ServerException
    {
        try
        {
            if (key.channel() instanceof SocketChannel clientChannel)
            {
                final Socket socket = clientChannel.socket();

                final CompletableFuture<Response> response = messageMediator.getResponseForChannel(clientChannel);
                if (response == null || !response.isDone())
                {
                    return;
                }

                LOG.debug("Handling WRITE for [{}]", socket.getRemoteSocketAddress());

                final var buffer = this.connections.getBuffer(socket);
                SocketUtils.writeToChannel(clientChannel, buffer, response.get().payload().toString());

                key.interestOps(SelectionKey.OP_READ);
                LOG.debug("Successfully handled WRITE for [{}]: {}", socket.getRemoteSocketAddress(), response);
                return;
            }

            throw new InvalidConnectionHandling("Invalid channel was opened for WRITE operation - " + key.channel());
        }
        catch (ServerException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to process WRITE operation", e);
        }
    }


    public void handleRead(SelectionKey key)
                    throws ServerException
    {
        if (key.channel() instanceof SocketChannel clientChannel)
        {
            final var socket = clientChannel.socket();
            LOG.debug("Handling read for [{}]", socket.getRemoteSocketAddress());

            try
            {
                final ByteBuffer buffer = this.connections.getBuffer(socket);
                final String data = SocketUtils.readFromChannel(clientChannel, buffer);
                final Request request = new Request(data, clientChannel);

                messageMediator.request(request);

                key.interestOps(SelectionKey.OP_WRITE);
                LOG.debug("READ finished for [{}]: {}", socket.getRemoteSocketAddress(), data);
            }
            catch (Exception e)
            {
                LOG.error("Connection issue. Closing channel [{}]", socket.getRemoteSocketAddress(), e);
                closeClientChannel(socket);

                throw new ServerException("Failed to process READ operation", e);
            }

            return;
        }

        throw new InvalidConnectionHandling("Invalid channel was opened for READ operation");
    }


    private void closeClientChannel(Socket socket)
                    throws ServerException
    {
        LOG.info("Closing client channel [{}]", socket.getRemoteSocketAddress());

        try
        {
            final Request closeRequest = new Request(CloseUserCommand.NAME, socket.getChannel());
            messageMediator.request(closeRequest);
        }
        catch (Exception e)
        {
            throw new ServerException("Failed to close channel", e);
        }
    }


    public void handleAccept(SelectionKey key)
                    throws ServerException
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
