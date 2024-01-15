package org.example.features.server;

import org.example.features.server.model.ServerClient;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerClientStorage {

    private static int historicalClients = 0;

    private static final List<ServerClient> clients = new ArrayList<>();

    public void addClient(String clientName, SocketChannel channel) {
        ServerClient client = new ServerClient(clientName, channel);
        clients.add(client);
        historicalClients++;
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
                .collect(Collectors.joining(", "));
    }

    public boolean isClientExists(String clientName) {
        return clients.stream()
                .anyMatch(client -> client.clientName().equals(clientName));
    }

    public boolean isClientExists(SocketChannel socketChannel) {
        return clients.stream()
                .anyMatch(client -> client.channel().equals(socketChannel));
    }

    public int size() {
        return clients.size();
    }

    public Optional<String> getClientName(SocketChannel client) {
        return clients.stream()
                .filter(serverClient -> serverClient.channel().equals(client))
                .findFirst()
                .map(ServerClient::clientName);
    }

    public String generateName() {
        return String.format("Client%s", historicalClients + 1);
    }

    public Stream<ServerClient> getServerClients() {
        return clients.stream();
    }

}
