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

public class Server implements Stoppable {

    private final UDPInitSender udpInitSender;
    private Thread senderThread;

    private Integer tcpPort;

    private boolean isActive = true;

    private static Map<String, SocketChannel> clients = new HashMap<>();

    public Server(Integer udpPort, Integer tcpPort) {
        System.out.println("Running server on port: " + tcpPort);
        udpInitSender = new UDPInitSender(udpPort, tcpPort);

        this.tcpPort = tcpPort;
    }

    public void run(String host, Integer port) {
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

        ByteBuffer buffer = ByteBuffer.allocate(256);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {

                SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    register(selector, serverSocket);
                }

                if (key.isReadable()) {
                    answerWithEcho(buffer, key);
                }
                iter.remove();
            }
        }

    }

    private static void answerWithEcho(ByteBuffer buffer, SelectionKey key)
            throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        try {
            Optional<String> messageOptional = readMessage(buffer, client);
            if (messageOptional.isPresent()) {
                handleMessage(messageOptional.get(), client);
            }
        } catch (IOException e) {
            System.out.printf("Error processing message: %s\n", e.getMessage());
            client.close();
        }
    }

    private static void handleMessage(String message, SocketChannel client) throws IOException {
        System.out.println("Received message from client: " + message);

        String[] words = message.split(" ");
        if (!Set.of("NAME", "EXIT").contains(words[0])) {
            String name = words[0];
            if (clients.containsKey(name)) {
                SocketChannel receiver = clients.get(name);
                receiver.write(ByteBuffer.wrap(message.getBytes()));
                client.write(ByteBuffer.wrap("Message sent successfully".getBytes()));
                return;
            } else {
                client.write(ByteBuffer.wrap("Name not found".getBytes()));
            }

            return;
        }


        if ("NAME".equals(words[0])) {
            String name = message.substring(words[0].length() + 1);

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

        if ("EXIT".equals(words[0])) {
            System.out.println("Connection closed by client: " + client.getRemoteAddress());
            client.close();
            return;
        }

        client.write(ByteBuffer.wrap("Message is invalid".getBytes()));
    }

    private static Optional<String> readMessage(ByteBuffer buffer, SocketChannel client) throws IOException {
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

    private static void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException {

        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from: " + client.getRemoteAddress());

    }

    @Override
    public void stop() {
        udpInitSender.stop();
        isActive = false;
    }

}
