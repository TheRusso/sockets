package org.example.features.server;

import org.example.Stoppable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class ServerService implements Stoppable {

    private final UDPInitSender udpInitSender;
    private Thread senderThread;
    private Integer port;
    private String host;
    private final ServerMessageHandler serverMessageHandler;

    private static ServerService instance;

    public static ServerService getInstance(Integer udpPort, Integer tcpPort, String host) {
        if (instance == null) {
            instance = new ServerService(udpPort, tcpPort, host);
        }

        return instance;
    }

    public ServerService(Integer udpPort, Integer tcpPort, String host) {
        System.out.println("Running server on port: " + tcpPort);
        udpInitSender = new UDPInitSender(udpPort, tcpPort);

        this.port = tcpPort;
        this.host = host;
        this.serverMessageHandler = new ServerMessageHandler();
    }

    public void listen() {
        senderThread = new Thread(udpInitSender);
        senderThread.start();

        try {
            startListeningClients(host, port);
        } catch (IOException e) {
            System.out.printf("Error processing connection: %s\n", e.getMessage());
            senderThread.interrupt();
        }
    }

    private void startListeningClients(String host, Integer port) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Non-blocking server started on port " + port);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                handleKey(key, selector, serverSocket);
                iter.remove();
            }
        }

    }

    private void handleKey(SelectionKey key, Selector selector, ServerSocketChannel serverSocket) throws IOException {
        if (key.isAcceptable()) {
            serverMessageHandler.register(selector, serverSocket);
        }

        if (key.isReadable()) {
            serverMessageHandler.handleReadKey(key);
        }
    }

    @Override
    public void stop() {
        udpInitSender.stop();
    }

}
