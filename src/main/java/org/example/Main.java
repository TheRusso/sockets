package org.example;

import org.example.features.client.ClientService;
import org.example.features.client.UDPInitListener;
import org.example.features.server.ServerService;

import java.io.IOException;
import java.util.Optional;

public class Main {

    private static final Integer UDP_PORT = 8067;
    private static final Integer TCP_PORT = 8068;

    private static final Integer UDP_TIMEOUT_MILLIS = 5000; // 5 seconds

    public static void main(String[] args) throws IOException {

        Optional<Integer> serverPort = UDPInitListener.listenForMessages(UDP_PORT, UDP_TIMEOUT_MILLIS);

        if (serverPort.isPresent()) {
            ClientService clientService = ClientService.getInstance("localhost", TCP_PORT);
            clientService.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                onShutdown(clientService);
            }));
        } else {
            ServerService serverService = ServerService.getInstance(UDP_PORT, TCP_PORT, "localhost");
            serverService.listen();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                onShutdown(serverService);
            }));
        }
    }

    private static void onShutdown(Stoppable stoppable) {
        System.out.println("Shut downing application...");
        if (stoppable != null) {
            stoppable.stop();
        }
    }

}
