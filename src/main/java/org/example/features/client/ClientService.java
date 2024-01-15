package org.example.features.client;

import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientService implements Stoppable {

    private static SocketChannel client;
    private static ByteBuffer buffer;
    private static ClientService instance;

    public static ClientService start(String host, Integer port) {
        if (instance == null) {
            instance = new ClientService(host, port);
        }

        return instance;
    }

    @Override
    public void stop() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            return;
        }
        buffer.flip();

        byte[] responseData = new byte[buffer.remaining()];
        buffer.get(responseData);

        System.out.println("Received response from server: " + new String(responseData));
    }

    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        client.write(buffer);
    }

}
