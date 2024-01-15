package org.example.features.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.Set;

public class ServerMessageHandler {

    private ByteBuffer buffer;
    private final ServerClientStorage serverClientStorage;

    public ServerMessageHandler() {
        this.buffer = ByteBuffer.allocate(256);
        this.serverClientStorage = new ServerClientStorage();
    }

    public void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from: " + client.getRemoteAddress());
    }

    public void handleReadKey(SelectionKey key)
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

    private void handleMessage(String receivedMessage, SocketChannel client) throws IOException {
        System.out.printf("%s: %s\n", client.getRemoteAddress(), receivedMessage);

        String[] words = receivedMessage.split(" ");
        if (words.length == 0) {
            sendMessageToClient(client, "There is no command");
            return;
        }

        String commandFromUser = words[0];
        String message = "";

        Set<String> commandsThatRequiresMessage = Set.of("NAME");
        if (commandsThatRequiresMessage.contains(commandFromUser) && words.length == 1) {
            sendMessageToClient(client, "There is no message");
            return;
        }

        if (words.length > 1) {
            message = receivedMessage.substring(commandFromUser.length() + 1);
        }


        if (!serverClientStorage.isClientExists(client) && !commandFromUser.equals("NAME")) {
            sendMessageToClient(client, "You are not registered, register first with command `NAME {your name}`");
            return;
        }

        Set<String> commands = Set.of("NAME", "QUIT", "STAT");
        if (commands.contains(commandFromUser)) {
            handleCommand(client, commandFromUser, message);
        } else {
            sendMessageToUser(client, commandFromUser, message);
        }
    }

    private void handleCommand(SocketChannel client, String commandFromUser, String message) throws IOException {
        switch (commandFromUser) {
            case "NAME" -> {
                if (serverClientStorage.isClientExists(message)) {
                    sendMessageToClient(client, "Name already exists");
                } else {
                    serverClientStorage.addClient(message, client);
                    sendMessageToClient(client, "Name registered successfully");
                    System.out.println("Client name: " + message);
                }
            }
            case "STAT" -> {
                String messageToClient = String.format("Clients: %s", serverClientStorage.getClientNames());
                sendMessageToClient(client, messageToClient);
            }
            case "QUIT" -> {
                System.out.println("Connection closed by client: " + client.getRemoteAddress());
                client.close();
            }
            default -> System.out.println("Command not found");
        }
    }

    private static void sendMessageToClient(SocketChannel client, String message) throws IOException {
        client.write(ByteBuffer.wrap(message.getBytes()));
    }

    private void sendMessageToUser(SocketChannel client, String commandFromUser, String clientMessage) throws IOException {
        Optional<SocketChannel> receiverOpt = serverClientStorage.getClient(commandFromUser);
        if (receiverOpt.isEmpty()) {
            sendMessageToClient(client, "User not found");
            return;
        }

        SocketChannel receiver = receiverOpt.get();

        if (receiver.equals(client)) {
            sendMessageToClient(client, "You can't send message to yourself");
            return;
        }

        String message = String.format("%s: %s", commandFromUser, clientMessage);
        sendMessageToClient(receiver, message);
        sendMessageToClient(client, "Message sent successfully");
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

}
