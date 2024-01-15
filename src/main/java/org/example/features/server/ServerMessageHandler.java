package org.example.features.server;

public class ServerMessageHandler {

    public void handle(String clientName, String message) {
        System.out.println("Message received: " + message);
    }

}
