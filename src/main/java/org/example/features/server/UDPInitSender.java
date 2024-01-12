package org.example.features.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPInitSender implements Runnable {

    private final int udpPort;
    private final int tcpPort;

    public UDPInitSender(int udpPort, int tcpPort) {
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }

    @Override
    public void run() {
        try {
            DatagramSocket senderSocket = new DatagramSocket();

            final int INTERVAL = 500;
            System.out.printf("UDPInitSender is using port: %s, interval: %d millis\n", udpPort, INTERVAL);

            while (!Thread.currentThread().isInterrupted()) {
                String message = Integer.toString(tcpPort);
                byte[] data = message.getBytes();

                InetAddress destinationAddress = InetAddress.getLocalHost();

                DatagramPacket packet = new DatagramPacket(data, data.length, destinationAddress, udpPort);

                senderSocket.send(packet);

                Thread.sleep(INTERVAL);
            }

            senderSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
            stop();
        }
    }

    public void stop() {
        Thread.currentThread().interrupt();
    }

}
