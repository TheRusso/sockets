package org.example.features.client;

import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client implements Stoppable {

    private static SocketChannel client;
    private static ByteBuffer buffer;
    private static Client instance;

    public static Client start() {
        if (instance == null) {
            instance = new Client();
        }

        return instance;
    }

    private Client() {
    }

    public void run() {
        String host = "127.0.0.1";
        int port = 8068;


        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(host, port));

            System.out.println("Connected to server: " + socketChannel.getRemoteAddress());

            // Send a message to the server
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();

                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                socketChannel.write(buffer);

                // Receive the response from the server
                ByteBuffer responseBuffer = ByteBuffer.allocate(256);
                int read = socketChannel.read(responseBuffer);
                if (read == -1) {
                    break;
                }
                responseBuffer.flip();

                byte[] responseData = new byte[responseBuffer.remaining()];
                responseBuffer.get(responseData);

                System.out.println("Received response from server: " + new String(responseData));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // sleep
    }

}
