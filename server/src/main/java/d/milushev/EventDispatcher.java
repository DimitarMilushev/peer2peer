//package main.java.d.milushev;
//
//
//import main.java.d.milushev.models.protocol.Response;
//
//import java.util.LinkedList;
//import java.util.Queue;
//
//
//// TODO: Make thread-safe
//public class EventDispatcher
//{
//    private static EventDispatcher instance;
//
//
//    private EventDispatcher(Queue<Operation> tasks)
//    {
//        this.tasks = tasks;
//    }
//
//
//    public static EventDispatcher getInstance()
//    {
//        if (instance == null)
//        {
//            instance = new EventDispatcher(new LinkedList<>());
//        }
//
//        return instance;
//    }
//
//
//    public Operation poll()
//    {
//        return tasks.poll();
//    }
//
//}
