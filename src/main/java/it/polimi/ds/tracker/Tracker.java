package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tracker {
    public static final int TRACKER_PORT = 2222; // will be taken from environment
    private ServerSocket serverSocket;
    private Storage storage = new Storage();
    private static final Logger logger = Logger.getLogger("Tracker");

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        tracker.start(TRACKER_PORT);
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                new ClientHandler(serverSocket.accept(), storage).start();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not accept replica request.");
        }
        stop();
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not close tracker properly.");
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private Storage storage;

        public ClientHandler(Socket socket, Storage storage) {
            this.clientSocket = socket;
            this.storage = storage;
        }

        @Override
        public void run() {
            try {
                TCPClient replica = new TCPClient(clientSocket);
                Message inputMessage = (Message) replica.in().readObject();
                switch (inputMessage.getType()) {
                    case ADD_REPLICA:
                        List<Address> otherReplicas = storage.getReplicas();
                        storage.addReplica(inputMessage.getAddress());
                        replica.out().writeObject(new Message(MessageType.SEND_OTHER_REPLICAS, storage.getReplicas()));
                        for (Address address : otherReplicas) {
                            TCPClient currentOtherReplica = TCPClient.connect(address);
                            currentOtherReplica.out().writeObject(new Message(MessageType.SEND_REPLICA, inputMessage.getAddress()));
                        }
                        break;
                    case ADD_CLIENT:
                        replica.out().writeObject(new Message(MessageType.SEND_REPLICA, storage.addClient()));
                        break;
                    case REMOVE_REPLICA:
                        storage.removeReplica(inputMessage.getAddress());
                        break;
                    case REMOVE_CLIENT:
                        storage.removeClient(inputMessage.getAddress());
                        break;
                    default:
                        logger.log(Level.WARNING, "Message type not found.");
                }
                replica.close();
                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                logger.log(Level.WARNING, "Communication with a replica interrupted.");
            }
        }
    }
}
