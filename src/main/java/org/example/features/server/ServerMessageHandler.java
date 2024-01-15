package org.example.features.server;

import org.example.features.server.model.Command;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.Set;

public class ServerMessageHandler {


    private final ServerClientStorage serverClientStorage;

    public ServerMessageHandler() {
        this.serverClientStorage = new ServerClientStorage();
    }

    public void handleMessage(SocketChannel client, String receivedMessage) throws IOException {
        System.out.printf("%s: %s\n", client.getRemoteAddress(), receivedMessage);

        String[] words = receivedMessage.split(" ");
        if (words.length == 0) {
            sendServerMessageToClient(client, "There is no command");
            return;
        }

        String commandFromUser = words[0];
        String message = "";

        Set<String> commandsThatRequiresMessage = Set.of("NAME");
        if (commandsThatRequiresMessage.contains(commandFromUser) && words.length == 1) {
            sendServerMessageToClient(client, "There is no message");
            return;
        }

        if (words.length > 1) {
            message = receivedMessage.substring(commandFromUser.length() + 1);
        }

        if (!serverClientStorage.isClientExists(client) && !commandFromUser.equals("NAME")) {
            sendServerMessageToClient(client, "You are not registered, register first with command `NAME {your name}`");
            return;
        }

        if (Command.exists(commandFromUser)) {
            handleCommand(client, commandFromUser, message);
        } else {
            sendMessageToUser(client, commandFromUser, message);
        }
    }

    private void handleCommand(SocketChannel client, String commandFromUser, String message) throws IOException {
        switch (commandFromUser) {
            case "NAME" -> {
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
            sendMessageToClient(client, "User not found", clientMessage);
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

    public void askForName(SocketChannel client) throws IOException {
        int size = serverClientStorage.size();
        String autoName = String.format("Client%s", size + 1);
        String message = String.format("Write your name(Enter to generate auto name `%s`): NAME {your name}", autoName);
        client.write(ByteBuffer.wrap(message.getBytes()));
    }

}
