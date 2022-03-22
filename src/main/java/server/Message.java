package server;

import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final String data;

    public Message(MessageType type) {
        this.type = type;
        data = null;
    }

    public Message(MessageType type, String date) {
        this.type = type;
        this.data = date;
    }

    public MessageType getType() {
        return type;
    }

    public String getData() {
        return data;
    }
}
