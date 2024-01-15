package org.example.features.server;

import org.example.features.server.model.Command;
import org.example.features.server.model.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class ServerMessageHandler {


    private final ServerClientStorage serverClientStorage;

    public ServerMessageHandler() {
        this.serverClientStorage = new ServerClientStorage();
    }

    public void handleMessage(SocketChannel client, String receivedMessage) throws IOException {
        System.out.printf("%s: %s\n", client.getRemoteAddress(), receivedMessage);

        // TODO: refactor this
        Message message = null;
        try {
            message = MessageParser.parse(receivedMessage);
        } catch (Exception e) {
            sendServerMessageToClient(client, e.getMessage());
            return;
        }

        if (isNotRegistered(client, message)) {
            String clientName = String.format("Client%s", serverClientStorage.size() + 1);
            serverClientStorage.addClient(clientName, client);
            sendServerMessageToClient(client, "You are registered with name: " + clientName);
        }

        if (Command.exists(message.getCommand())) {
            handleCommand(client, message.getCommand(), message.getMessage());
        } else {
            sendServerMessageToClient(client, "Unknown command, use `HELP` to see available commands");
        }
    }

    private boolean isNotRegistered(SocketChannel client, Message message) {
        return !serverClientStorage.isClientExists(client) && !Command.NAME.getName().equals(message.getCommand());
    }

    private void handleCommand(SocketChannel client, String commandFromUser, String message) throws IOException {
        final Optional<String> clientName = serverClientStorage.getClientName(client);
        switch (commandFromUser) {
            case "HELP" -> {
                String messageToClient = String.format("Available commands: %s", Command.getNames());
                sendServerMessageToClient(client, messageToClient);
            }
            case "NAME" -> {
                if (clientName.isPresent()) {
                    sendServerMessageToClient(client, "You are already registered, name: " + clientName.get());
                    return;
                }

                if (serverClientStorage.isClientExists(message)) {
                    sendServerMessageToClient(client, "Name already exists, choose another one");
                } else {
                    serverClientStorage.addClient(message, client);
                    sendServerMessageToClient(client, "Name registered successfully");
                    System.out.println("Client name: " + message);
                }
            }
            case "STAT" -> {
                String messageToClient = String.format("Clients: %s", serverClientStorage.getClientNames());
                sendServerMessageToClient(client, messageToClient);
            }
            case "QUIT" -> {
                System.out.println("Connection closed by client: " + client.getRemoteAddress());
                clientName.ifPresent(serverClientStorage::removeClient);
                client.close();
            }
            default -> sendServerMessageToClient(client, "Unknown command");
        }
    }

    private static void sendServerMessageToClient(SocketChannel client, String message) throws IOException {
        sendMessageToClient(client, "Server", message);
    }

    private static void sendMessageToClient(SocketChannel client, String senderName, String message) throws IOException {
        String messageToSend = getMessageToSend(senderName, message);
        client.write(ByteBuffer.wrap(messageToSend.getBytes()));
    }

    private static String getMessageToSend(String senderName, String message) {
        String messageToSend;
        if (senderName != null && !senderName.isEmpty()) {
            messageToSend = senderName + ": " + message;
        } else {
            messageToSend = message;
        }
        return messageToSend;
    }

    private void sendMessageToUser(SocketChannel client, String commandFromUser, String clientMessage) throws IOException {
        Optional<SocketChannel> receiverOpt = serverClientStorage.getClient(commandFromUser);
        if (receiverOpt.isEmpty()) {
            String message = "User not found: " + commandFromUser;
            sendServerMessageToClient(client, message);
            return;
        }

        SocketChannel receiver = receiverOpt.get();

        if (receiver.equals(client)) {
            sendMessageToClient(client, "You can't send message to yourself", clientMessage);
            return;
        }

        sendMessageToClient(receiver, commandFromUser, clientMessage);
        sendMessageToClient(client, "Message sent successfully", clientMessage);
    }

}
