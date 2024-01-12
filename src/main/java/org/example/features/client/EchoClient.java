package org.example.features.client;

import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class EchoClient implements Stoppable {
    private static SocketChannel client;
    private static ByteBuffer buffer;
    private static EchoClient instance;

    public static EchoClient start(String host, Integer port) {
        if (instance == null)
            instance = new EchoClient(host, port);

        return instance;
    }

    public void stop() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        buffer = null;
    }

    private EchoClient(String host, Integer port) {
        try {
            client = SocketChannel.open(new InetSocketAddress(host, port));
            buffer = ByteBuffer.allocate(256);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendMessage(String msg) {
        buffer.flip();
        buffer.clear();
        buffer.put(msg.getBytes(StandardCharsets.UTF_8));
        String response = null;
        try {
            client.write(buffer);
            buffer.clear();
            client.read(buffer);
            response = new String(buffer.array()).trim();
            System.out.println("response=" + response);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

}
