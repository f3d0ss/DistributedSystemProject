package it.polimi.ds.tracker;

import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class App {
    private static final int TRACKER_PORT = 777; // will be taken from environment
    private ServerSocket serverSocket;
    private Storage storage = new Storage();
    public static void main(String[] args) {
        App tracker = new App();
        tracker.start(TRACKER_PORT);
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true)
                new ClientHandler(serverSocket.accept(), storage).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private Storage storage;

        public ClientHandler(Socket socket, Storage storage) {
            this.clientSocket = socket;
            this.storage = storage;
        }

        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                Message inputMessage = (Message) in.readObject();
                switch (inputMessage.getType()) {
                    case ADD_REPLICA:
                        storage.addReplica(inputMessage.getAddress());
                        out.writeObject(new Message(MessageType.SEND_OTHER_REPLICAS, storage.getReplicas()));
                        break;
                    case ADD_CLIENT:
                        out.writeObject(new Message(MessageType.SEND_REPLICA, storage.addClient()));
                        break;
                    case REMOVE_REPLICA:
                        storage.removeReplica(inputMessage.getAddress());
                        break;
                    case REMOVE_CLIENT:
                        storage.removeClient(inputMessage.getAddress());
                        break;
                }
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}