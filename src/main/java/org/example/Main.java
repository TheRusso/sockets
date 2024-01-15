package org.example;

import org.example.features.client.ClientService;
import org.example.features.client.UDPInitListener;
import org.example.features.server.ServerService;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final Integer UDP_PORT = 8067;
    private static final Integer TCP_PORT = 8068;

    private static final Integer UDP_TIMEOUT_MILLIS = 5000; // 5 seconds

    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws IOException {

        Optional<Integer> serverPort = UDPInitListener.listenForMessages(UDP_PORT, UDP_TIMEOUT_MILLIS);

        if (serverPort.isPresent()) {
            ClientService clientService = ClientService.start("localhost", TCP_PORT);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                onShutdown(clientService);
            }));

            executorService.execute(() -> {
                try {
                    while (true) {
                        Scanner scanner = new Scanner(System.in);
                        String nextLine = scanner.nextLine();
                        clientService.sendMessage(nextLine);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            executorService.execute(() -> {
                try {
                    while (true) {
                        clientService.readMessage();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
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
