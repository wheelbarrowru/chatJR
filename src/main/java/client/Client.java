package client;

import server.Connection;
import server.ConsoleHelper;
import server.Message;
import server.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected;
    private final String UNEXPECTED_MESSAGE_TYPE = "Неожиданный тип сообщения";
    private final String CLIENT_EXCEPTION = "Произошла ошибка во время работы клиента.";

    public class SocketThread extends Thread {

        public void run() {
            try (Socket socket = new Socket(getServerAddress(), getServerPort())) {
                connection = new Connection(socket);

                clientHandshake();
                clientMainLoop();

            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);

                ConsoleHelper.writeMessage(CLIENT_EXCEPTION);
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.NAME_REQUEST) {
                    String name = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, name));

                } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;

                } else {
                    throw new IOException(UNEXPECTED_MESSAGE_TYPE);
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());

                } else if (MessageType.USER_ADDED == message.getType()) {
                    informAboutAddingNewUser(message.getData());

                } else if (MessageType.USER_REMOVED == message.getType()) {
                    informAboutDeletingNewUser(((Message) message).getData());

                } else {
                    throw new IOException(UNEXPECTED_MESSAGE_TYPE);
                }
            }
        }

    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера: ");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт: ");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя: ");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            this.clientConnected = false;
            ConsoleHelper.writeMessage(CLIENT_EXCEPTION);
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage(CLIENT_EXCEPTION + "lol");
            return;
        }

        if (clientConnected) ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage(CLIENT_EXCEPTION);

        while (clientConnected) {
            String text = ConsoleHelper.readString();
            if ("exit".equalsIgnoreCase(text)) break;

            if (shouldSendTextFromConsole()) sendTextMessage(text);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

}
