package it.polimi.ds.tracker;

import it.polimi.ds.network.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the main Server, which keeps track of all the Replicas in the network.
 */
public class Tracker {
    private static final Logger logger = Logger.getLogger("Tracker");
    private static int minDelay = 0;
    private static int maxDelay = 0;
    private final Storage storage = new Storage();
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        if (args.length >= 3)
            tracker.start(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        else if (args.length >= 1)
            tracker.start(args[0]);
        else {
            logger.log(Level.SEVERE, "Too few arguments, tracker was not launched.");
            logger.log(Level.SEVERE, () -> "Please relaunch the tracker with " +
                    "<trackerPort> [<minDelay> <maxDelay>] as parameters.");
        }
    }

    private static int getChoice() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return Integer.parseInt(reader.readLine());
        } catch (NumberFormatException | IOException e) {
            return -1;
        }
    }

    private static void setMinDelay(int minDelay) {
        if (minDelay > 0 && minDelay <= Tracker.maxDelay)
            Tracker.minDelay = minDelay;
    }

    private static void setMaxDelay(int maxDelay) {
        if (maxDelay > 0)
            Tracker.maxDelay = maxDelay;
    }

    private void start(String port, int minDelay, int maxDelay) {
        Tracker.setMaxDelay(maxDelay);
        Tracker.setMinDelay(minDelay);
        this.start(port);
    }

    private void start(String port) {
        Thread tracker = new Thread(() -> runTracker(Integer.parseInt(port)));
        tracker.start();
        do {
            logger.log(Level.INFO, "Press 1 to close the Tracker.");
        }
        while (getChoice() != 1);
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not close the tracker properly.");
        }
        tracker.interrupt();
        logger.log(Level.INFO, "The tracker is now closed.");
    }

    private void runTracker(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                new ClientHandler(serverSocket.accept(), storage).start();
            }
        } catch (IOException e) {
            // This exception must be ignored, it happens when the main thread interrupts this one
        }
        stop();
    }

    private void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not close tracker properly.");
        }
    }

    /**
     * Represents the Thread that will handle the various requests of both Replicas and Clients.
     */
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final Storage storage;

        public ClientHandler(Socket socket, Storage storage) {
            this.clientSocket = socket;
            this.storage = storage;
        }

        @Override
        public void run() {
            try {
                TCPClient replica = new TCPClient(clientSocket);
                Message inputMessage = (Message) replica.in().readObject();
                List<Address> otherReplicas;
                int newTrackerIndex;
                switch (inputMessage.getType()) {
                    case ADD_REPLICA:
                        storage.lock();
                        otherReplicas = storage.getReplicas();
                        storage.addReplica(inputMessage.getAddress());
                        newTrackerIndex = storage.incrementAndGetTrackerIndex();
                        storage.unlock();
                        for (Address address : otherReplicas) {
                            new MessageSender(new Message(MessageType.SEND_NEW_REPLICA, inputMessage.getAddress(), newTrackerIndex), address).start();
                        }
                        SimulateDelay.uniform(minDelay, maxDelay);
                        replica.out().writeObject(new Message(MessageType.SEND_OTHER_REPLICAS, otherReplicas, newTrackerIndex));
                        logger.log(Level.INFO, () -> "Successfully connected with Replica " + inputMessage.getAddress().toString() + ".");
                        break;
                    case ADD_CLIENT:
                        SimulateDelay.uniform(minDelay, maxDelay);
                        replica.out().writeObject(new Message(MessageType.SEND_REPLICA, storage.addClient()));
                        break;
                    case REMOVE_REPLICA:
                        storage.lock();
                        storage.removeReplica(inputMessage.getAddress());
                        otherReplicas = storage.getReplicas();
                        newTrackerIndex = storage.incrementAndGetTrackerIndex();
                        storage.unlock();
                        for (Address address : otherReplicas) {
                            new MessageSender(new Message(MessageType.REMOVE_OLD_REPLICA, inputMessage.getAddress(), newTrackerIndex), address).start();
                        }
                        logger.log(Level.INFO, () -> "Successfully disconnected with Replica " + inputMessage.getAddress().toString() + ".");
                        break;
                    case REMOVE_CLIENT:
                        storage.removeClient(inputMessage.getAddress());
                        break;
                    default:
                        logger.log(Level.WARNING, "Message type not found.");
                }
                replica.close();
                clientSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Communication with a replica interrupted.");
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Could not read the message properly.");
            }
        }
    }

    /**
     * Represents the Thread that will broadcast the Messages regarding insertion or deletion of Replicas.
     */
    private static class MessageSender extends Thread {
        private final Message message;
        private final Address to;

        public MessageSender(Message message, Address to) {
            this.message = message;
            this.to = to;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    SimulateDelay.uniform(minDelay, maxDelay);
                    TCPClient currentOtherReplica = TCPClient.connect(to);
                    currentOtherReplica.out().writeObject(message);
                    currentOtherReplica.close();
                    return;
                } catch (IOException e) {
                    logger.log(Level.WARNING, () -> Thread.currentThread().toString() + ": Communication with replica " + to + " interrupted, retrying.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
