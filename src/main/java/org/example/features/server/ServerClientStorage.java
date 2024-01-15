package org.example.features.server;

import org.example.features.server.model.ServerClient;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerClientStorage {

    private final List<ServerClient> clients = new ArrayList<>();

    public void addClient(String clientName, SocketChannel channel) {
        ServerClient client = new ServerClient(clientName, channel);
        clients.add(client);
    }

    public void removeClient(String clientName) {
        clients.removeIf(client -> client.clientName().equals(clientName));
    }

    public Optional<SocketChannel> getClient(String clientName) {
        return clients.stream()
                .filter(client -> client.clientName().equals(clientName))
                .findFirst()
                .map(ServerClient::channel);
    }

    public String getClientNames() {
        return clients.stream()
                .map(ServerClient::clientName)
                .reduce("", (acc, clientName) -> acc + clientName + "\n");
    }

    public boolean isClientExists(String clientName) {
        return clients.stream()
                .anyMatch(client -> client.clientName().equals(clientName));
    }

    public boolean isClientExists(SocketChannel socketChannel) {
        return clients.stream()
                .anyMatch(client -> client.channel().equals(socketChannel));
    }


}
