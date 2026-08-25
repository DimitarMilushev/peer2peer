package main.java.d.milushev.p2p.server.commands;


import d.milushev.p2p.network_utils.models.Request;
import d.milushev.p2p.network_utils.models.Response;
import d.milushev.p2p.network_utils.models.ResponseFuture;
import main.java.d.milushev.p2p.server.exceptions.processor.BadSyntaxException;
import main.java.d.milushev.p2p.server.exceptions.processor.UnsupportedInputException;
import main.java.d.milushev.p2p.server.listener.MessageMediator;
import main.java.d.milushev.p2p.server.repositories.InMemoryClientsRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class CommandProcessor implements Runnable, Closeable
{
    private static final Logger LOG = LogManager.getLogger(CommandProcessor.class);

    private final Executor executor;
    private final InMemoryClientsRepository repository;
    private final MessageMediator messageMediator;
    private final Consumer<Socket> onCloseUser;

    private volatile boolean isStopped;


    public CommandProcessor(MessageMediator messageMediator, Consumer<Socket> onCloseUser)
    {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.messageMediator = messageMediator;
        this.onCloseUser = onCloseUser;

        repository = new InMemoryClientsRepository();
        isStopped = true;
    }


    @Override
    public void run()
    {
        LOG.info("Starting Command Processor...");

        isStopped = false;
        Request request;
        while (!isStopped)
        {
            try
            {
                request = messageMediator.poll();
            }
            catch (InterruptedException e)
            {
                LOG.error("Error while polling for messages: ", e);
                Thread.currentThread().interrupt();
                continue;
            }

            if (request == null)
            {
                continue;
            }

            try
            {
                process(request);
            }
            catch (Exception e)
            {
                LOG.error("Unexpected error while processing", e);

                final var error = new CompletableFuture<Response>();
                error.complete(new Response(e, 400, request.channel()));

                messageMediator.respond(new ResponseFuture(request.channel(), error));
            }
        }

        LOG.info("Shutting down processor");
    }


    private void process(Request request)
                    throws Exception
    {
        LOG.debug("Processing request [{}]", request.payload());

        final String input = request.payload().toString();
        if (input.isBlank())
        {
            throw new BadSyntaxException(input);
        }

        final String command = input.split(" ")[0];
        LOG.debug("Parsed command [{}] from request [{}]", command, input);
        switch (command)
        {
            case CloseUserCommand.NAME:
                executor.execute(new CloseUserCommand(request.channel().socket(), repository, onCloseUser));
                break;
            case ListFilesCommand.NAME:
                executor.execute(new ListFilesCommand(request.channel().socket(), repository, messageMediator::respond));
                break;
            case RegisterCommand.NAME:
                executor.execute(new RegisterCommand(input, request.channel().socket(), repository, messageMediator::respond));
                break;
            case UnregisterCommand.NAME:
                executor.execute(new UnregisterCommand(input, request.channel().socket(), repository, messageMediator::respond));
                break;
            case ListActiveUsersCommand.NAME:
                executor.execute(new ListActiveUsersCommand(request.channel().socket(), repository, messageMediator::respond));
                break;
            default:
                throw new UnsupportedInputException(input);
        }
    }


    @Override
    public void close()
    {
        LOG.info("Closing Command Processor...");
        isStopped = true;

        try
        {
            Thread.currentThread().join();
        }
        catch (InterruptedException e)
        {
            LOG.error("Error while waiting for Command Processor to stop", e);
        }
    }
}
