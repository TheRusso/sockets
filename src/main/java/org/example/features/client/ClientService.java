package org.example.features.client;

import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientService implements Stoppable {

    private static SocketChannel client;
    private static ByteBuffer buffer;
    private static ClientService instance;
    private static final Scanner scanner = new Scanner(System.in);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public static ClientService getInstance(String host, Integer port) {
        if (instance == null) {
            instance = new ClientService(host, port);
        }

        return instance;
    }

    public void start() {
        executorService.execute(() -> {
            try {
                while (true) {
                    if (System.in.available() > 0) {
                        String message = scanner.next();
                        sendMessage(message);
                        if ("QUIT".equals(message)) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                stop();
            }
        });
        executorService.execute(() -> {
            try {
                while (true) {
                    readMessage();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                stop();
            }
        });
    }

    @Override
    public void stop() {
        scanner.close();
        executorService.shutdownNow();
        buffer = null;
    }

    private ClientService(String host, Integer port) {
        try {
            client = SocketChannel.open(new InetSocketAddress(host, port));
            System.out.println("Connected to server: " + client.getRemoteAddress());
            buffer = ByteBuffer.allocate(256);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage() throws IOException {
        buffer.clear();
        int read = client.read(buffer);
        if (read == -1) {
            client.close();
            throw new RuntimeException("Connection closed");
        }
        buffer.flip();

        byte[] responseData = new byte[buffer.remaining()];
        buffer.get(responseData);

        System.out.println("> " + new String(responseData));
    }

    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        client.write(buffer);
    }

}
