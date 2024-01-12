package org.example.features.server;

import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ServerService implements Stoppable {

    private final UDPInitSender udpInitSender;
    private Thread senderThread;
    private Integer port;
    private String host;
    private ByteBuffer buffer;

    private static Map<String, SocketChannel> clients = new HashMap<>();

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

        buffer.clear();
        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                handleKey(key, selector, serverSocket);
                iter.remove();
            }
        }

    }

    private void handleKey(SelectionKey key, Selector selector, ServerSocketChannel serverSocket) throws IOException {
        if (key.isAcceptable()) {
            register(selector, serverSocket);
        }

        if (key.isReadable()) {
            handleReadKey(key);
        }
    }

    private void handleReadKey(SelectionKey key)
            throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        try {
            Optional<String> messageOptional = readMessage(client);
            if (messageOptional.isPresent()) {
                handleMessage(messageOptional.get(), client);
            }
        } catch (IOException e) {
            System.out.printf("Error processing message: %s\n", e.getMessage());
            client.close();
        }
    }

    private static void handleMessage(String message, SocketChannel client) throws IOException {
        System.out.printf("%s: %s", client.getRemoteAddress(), message);

        String[] words = message.split(" ");
        String command = words[0];
        if (!Set.of("NAME", "EXIT").contains(command)) {
            if (clients.containsKey(command)) {
                SocketChannel receiver = clients.get(command);
                receiver.write(ByteBuffer.wrap(message.getBytes()));
                client.write(ByteBuffer.wrap("Message sent successfully".getBytes()));
                return;
            } else {
                client.write(ByteBuffer.wrap("Name not found".getBytes()));
            }

            return;
        }


        if ("NAME".equals(command)) {
            String name = message.substring(command.length() + 1);

            if (clients.containsValue(client)) {
                client.write(ByteBuffer.wrap("You already registered".getBytes()));
                return;
            }

            if (clients.containsKey(name)) {
                client.write(ByteBuffer.wrap("Name already exists".getBytes()));
                return;
            } else {
                clients.put(name, client);
                client.write(ByteBuffer.wrap("Name registered successfully".getBytes()));
            }

            System.out.println("Client name: " + name);
            return;
        }

        if ("EXIT".equals(command)) {
            System.out.println("Connection closed by client: " + client.getRemoteAddress());
            client.close();
            return;
        }

        client.write(ByteBuffer.wrap("Message is invalid".getBytes()));
    }

    private Optional<String> readMessage(SocketChannel client) throws IOException {
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

    private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
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