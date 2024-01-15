package org.example.features.server;


import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class ServerService implements Stoppable {

    private final UDPInitSender udpInitSender;
    private Thread senderThread;
    private Integer port;
    private String host;
    private ByteBuffer buffer;
    private final ServerMessageHandler serverMessageHandler;

    private static ServerService instance;

    public static ServerService getInstance(Integer udpPort, Integer tcpPort, String host) {
        if (instance == null) {
            instance = new ServerService(udpPort, tcpPort, host);
        }

        return instance;
    }

    public ServerService(Integer udpPort, Integer tcpPort, String host) {
        System.out.println("Running server on port: " + tcpPort);
        udpInitSender = new UDPInitSender(udpPort, tcpPort);

        this.port = tcpPort;
        this.host = host;
        this.buffer = ByteBuffer.allocate(256);
        this.serverMessageHandler = new ServerMessageHandler();


        System.out.println("   _____                          \n" +
                "  / ____|                         \n" +
                " | (___   ___ _ ____   _____ _ __ \n" +
                "  \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\n" +
                "  ____) |  __/ |   \\ V /  __/ |   \n" +
                " |_____/ \\___|_|    \\_/ \\___|_|   \n" +
                "                                  \n" +
                "                                  ");
    }

    public void listen() {
        senderThread = new Thread(udpInitSender);
        senderThread.start();

        try {
            startListeningClients(host, port);
        } catch (IOException e) {
            System.out.printf("Error processing connection: %s\n", e.getMessage());
            senderThread.interrupt();
        }
    }

    private void startListeningClients(String host, Integer port) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Non-blocking server started on port " + port);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                try {
                    handleKey(key, selector, serverSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                iter.remove();
            }
        }
    }

    private void handleKey(SelectionKey key, Selector selector, ServerSocketChannel serverSocket) throws IOException {
        if (key.isAcceptable()) {
            register(selector, serverSocket);
        }

        if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            Optional<String> messageOptional = readMessage(client);
            if (messageOptional.isPresent()) {
                serverMessageHandler.handleMessage(client, messageOptional.get());
            }
        }
    }

    private Optional<String> readMessage(SocketChannel client) throws IOException {
        buffer.clear();
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by the client
            System.out.println("Connection closed by client: " + client.getRemoteAddress());
            client.close();
        } else if (bytesRead > 0) {
            // Process the received data
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

//            client.write(ByteBuffer.wrap(data));
            buffer.clear();
            return Optional.of(new String(data));
        }

        return Optional.empty();
    }

    private void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from: " + client.getRemoteAddress());
    }

    @Override
    public void stop() {
        udpInitSender.stop();
    }

}
