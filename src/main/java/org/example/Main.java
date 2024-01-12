package org.example;

import org.example.features.client.Client;
import org.example.features.client.UDPInitListener;
import org.example.features.server.Server;

import java.io.IOException;
import java.util.Optional;

public class Main {

    private static final Integer UDP_PORT = 8067;
    private static final Integer TCP_PORT = 8068;

    private static final Integer UDP_TIMEOUT_MILLIS = 5000; // 5 seconds


    public static void main(String[] args) throws IOException, InterruptedException {

        Optional<Integer> serverPort = UDPInitListener.listenForMessages(UDP_PORT, UDP_TIMEOUT_MILLIS);

        if (serverPort.isPresent()) {
            Client client = new Client();
            client.run();
//            EchoClient echoClient = EchoClient.start("localhost", TCP_PORT);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                onShutdown(client);
            }));
        } else {
            Server server = new Server(UDP_PORT, TCP_PORT);
            server.run("localhost", TCP_PORT);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                onShutdown(server);
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
