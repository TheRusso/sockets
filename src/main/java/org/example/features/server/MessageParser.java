package org.example.features.server;

import org.example.features.server.model.Command;
import org.example.features.server.model.Message;

import java.util.Set;

public class MessageParser {

    public static Message parse(String receivedMessage) {
        String[] words = receivedMessage.split(" ");
        if (words.length == 0) {
            throw new IllegalArgumentException("There is no command");
        }

        String commandFromUser = words[0];
        String message = "";

        Command command = Command.of(commandFromUser);
        Set<Command> commandsThatNotRequiresMessage = Set.of(Command.STAT, Command.QUIT, Command.HELP);
        // TODO: add to validation service
        if (commandsThatNotRequiresMessage.contains(command) && words.length > 1) {
            throw new IllegalArgumentException("Command does not require message");
        }

        if (!commandsThatNotRequiresMessage.contains(command) && words.length == 1) {
            throw new IllegalArgumentException("Command requires message");
        }

        if (words.length > 1) {
            message = receivedMessage.substring(commandFromUser.length() + 1);
        }

        return new Message(commandFromUser, message);
    }

}
