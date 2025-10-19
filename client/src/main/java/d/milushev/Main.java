package main.java.d.milushev;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Executors;


public class Main
{
    static volatile String msg = "register Gosho myFile1";

    public static void main(String[] args) throws IOException
    {
        var e = Executors.newSingleThreadExecutor();
        e.submit(Main::readConsole);

        try (Selector selector = Selector.open(); SocketChannel clientChannel = SocketChannel.open();)
        {
            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress(8000));
            clientChannel.register(selector, SelectionKey.OP_CONNECT);

            ByteBuffer writeBuffer = ByteBuffer.allocate(1024);

            System.out.println("Client started. Type messages to send:");
            while (!msg.equalsIgnoreCase("exit"))
            {
                if (selector.select(1000) == 0)
                {
                    continue;
                }

                for (var key : selector.selectedKeys())
                {
                    if (key.isWritable())
                    {
                        if (msg.isBlank()) continue;

                        writeBuffer.clear();
                        writeBuffer.put(msg.getBytes(StandardCharsets.UTF_8));

                        writeBuffer.flip();

                        while (writeBuffer.hasRemaining())
                        {
                            clientChannel.write(writeBuffer);
                        }

                        System.out.println("Client: " + msg);
                        writeBuffer.clear();
                        msg = "";
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    else if (key.isReadable())
                    {
                        final var sb = new StringBuffer();
                        writeBuffer.clear();

                        var bytesRead = clientChannel.read(writeBuffer);
                        writeBuffer.flip();

                        while (bytesRead > 0)
                        {
                            sb.append(new String(writeBuffer.array(), writeBuffer.position(), bytesRead));
                            bytesRead = clientChannel.read(writeBuffer);
                        }

                        System.out.println("Server: " + sb);
                        writeBuffer.clear();
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else if (key.isConnectable())
                    {
                        while (!clientChannel.finishConnect())
                        {
                            System.out.println("Connecting...");
                        }
                        System.out.println("Connected");
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                selector.selectedKeys().clear();
            }
        }

        e.shutdown();
    }

    public static void readConsole()
    {
        final Scanner scn = new Scanner(System.in);

        while (!msg.equalsIgnoreCase("exit"))
        {
            msg = scn.nextLine();
        }
    }
}
