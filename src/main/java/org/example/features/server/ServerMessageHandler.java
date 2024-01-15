package org.example.features.server;

import org.example.features.server.model.Command;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import static java.util.Arrays.copyOfRange;

public class ServerMessageHandler {


    private final ServerClientStorage serverClientStorage;

    public ServerMessageHandler() {
        this.serverClientStorage = new ServerClientStorage();
    }

    public void handleMessage(SocketChannel client, String receivedMessage) throws IOException {
        System.out.printf("%s: %s\n", client.getRemoteAddress(), receivedMessage);

        String[] args = receivedMessage.split(" ");
        if (args.length == 0) {
            sendServerMessageToClient(client, "Message should not be empty");
            return;
        }

        String command = args[0];
        if (command == null || command.isEmpty()) {
            sendServerMessageToClient(client, "There is no command");
            return;
        }

        if (isNotRegistered(client, command)) {
            String clientName = String.format("Client%s", serverClientStorage.size() + 1);
            registerName(client, clientName);
        }

        if (Command.exists(command)) {
            handleCommand(client, command, copyOfRange(args, 1, args.length));
        } else {
            sendServerMessageToClient(client, "Unknown command, use `HELP` to see available commands");
        }
    }

    private void registerName(SocketChannel client, String name) throws IOException {
        serverClientStorage.addClient(name, client);
        sendServerMessageToClient(client, "You are registered with name: " + name);
    }

    private boolean isNotRegistered(SocketChannel client, String command) {
        return !serverClientStorage.isClientExists(client) && !Command.NAME.getName().equals(command);
    }

    private void handleCommand(SocketChannel client, String commandFromUser, String[] args) throws IOException {
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
                String message = args[0];

                if (serverClientStorage.isClientExists(message)) {
                    sendServerMessageToClient(client, "Name already exists, choose another one");
                } else {
                    registerName(client, message);
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
            case "MESG" -> {
                String receiverName = args[0];
                String message = String.join(" ", copyOfRange(args, 1, args.length));

                sendMessageToUser(client, receiverName, message);
            }
            case "BCST" -> {
                if (clientName.isEmpty()) {
                    sendServerMessageToClient(client, "You are not registered, use `NAME` command to register");
                    return;
                }
                String message = String.join(" ", args);

                serverClientStorage.getServerClients()
                        .filter(sc -> !sc.channel().equals(client))
                        .forEach(sc -> {
                            try {
                                sendMessageToClient(sc.channel(), clientName.get(), message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
            case "IPAD" -> {
                String receiverName = args[0];
                if (!serverClientStorage.isClientExists(receiverName)) {
                    sendServerMessageToClient(client, "User not found: " + receiverName);
                    return;
                }

                SocketChannel receiver = serverClientStorage.getClient(receiverName).get();
                String messageToClient = String.format("IP address: %s", receiver.getRemoteAddress());
                sendServerMessageToClient(client, messageToClient);
            }
            case "KILL" -> {
                String receiverName = args[0];
                if (!serverClientStorage.isClientExists(receiverName)) {
                    sendServerMessageToClient(client, "User not found: " + receiverName);
                    return;
                }

                SocketChannel receiver = serverClientStorage.getClient(receiverName).get();
                String messageToClient = String.format("You are killed by: %s", clientName.get());
                sendServerMessageToClient(receiver, messageToClient);
                receiver.close();
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

    private void sendMessageToUser(SocketChannel client, String receiverName, String clientMessage) throws IOException {
        Optional<SocketChannel> receiverOpt = serverClientStorage.getClient(receiverName);
        if (receiverOpt.isEmpty()) {
            String message = "User not found: " + receiverName;
            sendServerMessageToClient(client, message);
            return;
        }

        SocketChannel receiver = receiverOpt.get();

        if (receiver.equals(client)) {
            sendServerMessageToClient(client, "You can't send message to yourself");
            return;
        }

        sendMessageToClient(receiver, receiverName, clientMessage);
        sendMessageToClient(client, "Message sent successfully", clientMessage);
    }

}
