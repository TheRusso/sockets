package org.example.features.server.commands;

import java.nio.channels.SocketChannel;

public class ExitCommandHandler {

    public static String exit(String command, SocketChannel channel) {
        System.out.println("Client disconnected");
        return "Bye!";
    }

}
