package main.java.d.milushev;

import main.java.d.milushev.models.commands.CloseUserCommand;
import main.java.d.milushev.models.commands.ListFilesCommand;
import main.java.d.milushev.models.commands.RegisterCommand;
import main.java.d.milushev.models.commands.UnregisterCommand;
import main.java.d.milushev.models.protocol.Request;
import main.java.d.milushev.models.protocol.Response;
import main.java.d.milushev.models.protocol.ResponseFuture;
import main.java.d.milushev.repository.InMemoryClientsRepository;

import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements Runnable {
    private final BlockingQueue<Request> requests;
    private final BlockingQueue<ResponseFuture> responses;
    private final BlockingQueue<Exception> errors;
    private final Executor executor;
    private final InMemoryClientsRepository repository;
    private final AtomicBoolean stopSignal;

    public Processor(AtomicBoolean stopSignal) {
        this.stopSignal = stopSignal;

        repository = new InMemoryClientsRepository();
        executor = Executors.newVirtualThreadPerTaskExecutor();
        responses = new LinkedBlockingQueue<>();
        requests = new LinkedBlockingQueue<>();
        errors = new LinkedBlockingQueue<>();
    }

    public void enqueue(Request request) {
        this.requests.add(request);
    }


    public CompletableFuture<Response> getResponseForChannel(SocketChannel channel) {
        final var future = responses.peek();
        if (future == null || future.channel() != channel) {
            return null;
        }

        return responses.poll().response();
    }

    @Override
    public void run() {
        System.out.println("Started processor");

        Request req;
        while (!stopSignal.get()) {
            req = requests.poll();

            if (req == null) {
                continue;
            }

            try {
                process(req);
            } catch (Exception e) {
                System.out.println("Error while processing [" + e + "]");

                final var error = new CompletableFuture<Response>();
                error.complete(new Response(e, 400, req.channel()));

                responses.add(new ResponseFuture(req.channel(), error));
            }
        }

        System.out.println("Shutting down processor");
    }

    private void process(Request request) throws Exception {
        final String input = request.path();
        if (input.isBlank()) {
            throw new Exception("Bad syntax [" + input + "]");
        }

        final String command = input.split(" ")[0];
        switch (command) {
            case CloseUserCommand.NAME:
                executor.execute(new CloseUserCommand(request.channel().socket(), repository, errors));
                break;
            case ListFilesCommand.NAME:
                executor.execute(new ListFilesCommand(request.channel().socket(), repository, errors, responses));
                break;
            case RegisterCommand.NAME:
                executor.execute(new RegisterCommand(input, request.channel().socket(), repository, errors, responses));
                break;
            case UnregisterCommand.NAME:
                executor.execute(new UnregisterCommand(input, request.channel().socket(), repository, errors, responses));
                break;
            default:
                throw new Exception("Unrecognized input [" + input + "]");
        }
    }
}

