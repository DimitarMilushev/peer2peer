//package main.java.d.milushev;
//
//
//import main.java.d.milushev.models.commands.SlowHelloCommand;
//import main.java.d.milushev.models.protocol.Request;
//import main.java.d.milushev.models.protocol.Response;
//import main.java.d.milushev.models.protocol.ResponseFuture;
//
//import java.util.LinkedList;
//import java.util.Queue;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//
//
//public class DemoProcessor implements Runnable, AutoCloseable
//{
//    private final Executor executor;
//
//    private final Queue<Request> requests;
//    private final Queue<ResponseFuture> responses;
//
//    public DemoProcessor(Queue<Request> requests, Queue<ResponseFuture> responses)
//    {
//        this.responses = responses;
//        this.requests = requests;
//
//        this.executor = Executors.newVirtualThreadPerTaskExecutor();
//    }
//
//
//    @Override
//    public void run()
//    {
//        while (true)
//        {
//            while (!requests.isEmpty())
//            {
//                final var cmd = create(requests.poll());
//
//                executor.execute(cmd);
//            }
//        }
//    }
//
//    private SlowHelloCommand create(Request request)
//    {
//        return new SlowHelloCommand(responses, request);
//    }
//
//
//
//    @Override
//    public void close() throws Exception
//    {
//
//    }
//}
