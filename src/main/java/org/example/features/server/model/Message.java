package org.example.features.server.model;

public class Message {

    private String command;
    private String message;

    public Message(String sender, String message) {
        this.command = sender;
        this.message = message;
    }

    public String getCommand() {
        return command;
    }

    public String getMessage() {
        return message;
    }

}
