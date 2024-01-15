package org.example.features.server.model;

import java.nio.channels.SocketChannel;

public record ServerClient(
        String clientName,
        SocketChannel channel
) {

}
