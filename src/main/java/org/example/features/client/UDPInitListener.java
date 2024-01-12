package org.example.features.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

public class UDPInitListener {

    public static Optional<Integer> listenForMessages(int listenPort, int timeoutMillis) {


        try (DatagramSocket socket = new DatagramSocket(listenPort, InetAddress.getLocalHost())) {
            socket.setSoTimeout(timeoutMillis);

            System.out.println("UDP listener started on port " + listenPort);

            byte[] buffer = new byte[1024];

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    byte[] data = packet.getData();

                    String receivedData = new String(data, 0, packet.getLength());
                    System.out.println("Received UDP data: " + receivedData);

                    return Optional.of(ByteBuffer.wrap(data).getInt());
                } catch (java.net.SocketTimeoutException e) {
                    System.out.println("No UDP messages received within the timeout");
                    return Optional.empty();
                } catch (Exception e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
