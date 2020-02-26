package it.polimi.ds.replica;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Replica {
    private Address replicaAddress;
    private Address trackerAddress;
    private List<Address> otherReplicaAddresses;
    private StateHandler state;
    private ServerSocket serverSocket;
    private static final Logger logger = Logger.getLogger("Replica");

    public static void main(String[] args) {
        Replica tracker = new Replica();
        tracker.start(args[0], args[1], args[2], args[3]);
    }

    public void start(String trackerIp, String trackerPort, String replicaIp, String replicaPort) {
        int trackerIndex = 0;
        this.trackerAddress = new Address(trackerIp, Integer.valueOf(trackerPort));
        this.replicaAddress = new Address(replicaIp, Integer.valueOf(replicaPort));
        while (trackerIndex == 0){
            try {
                trackerIndex = joinNetwork(TCPClient.connect(trackerAddress));
            } catch (IOException | ClassNotFoundException e) {
                logger.log(Level.WARNING, "Impossible to contact the server, exiting.");
            }
        }
//        Try to get the state from one of the replicas
        for (int i = 0; state == null ; i++) {
            Address otherReplica = otherReplicaAddresses.get(i % otherReplicaAddresses.size());
            try {
                state = getState(TCPClient.connect(otherReplica), trackerIndex);
            } catch (IOException | ClassNotFoundException e) {
                logger.log(Level.WARNING, () -> "Impossible to get a valid state from " + otherReplicaAddresses + ", trying an other one.");
            }
        }

//        Here I have the state
        Thread replica = new Thread(() -> runReplica(replicaPort));
        replica.start();
        do {
            logger.log(Level.INFO, "Press 1 to close the Tracker");
        }
        while (getChoice() != 1);
        try {
            TCPClient tracker = TCPClient.connect(trackerAddress);
            tracker.out().writeObject(new Message(MessageType.REMOVE_REPLICA, replicaAddress));
            tracker.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not inform the tracker of the replica closure.");
        }
        replica.interrupt();
    }

    private static int getChoice() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return Integer.parseInt(reader.readLine());
        } catch (NumberFormatException | IOException e) {
            return -1;
        }
    }

    private void runReplica(String replicaPort) {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(replicaPort));
            while (true) {
                new IncomingMessageHandler(serverSocket.accept(), state).start();
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


    private int joinNetwork(TCPClient client) throws IOException, ClassNotFoundException {
        client.out().writeObject(new Message(MessageType.ADD_REPLICA, replicaAddress));
        otherReplicaAddresses = ((Message) client.in().readObject()).getAddressSet();
        return ((Message) client.in().readObject()).getTrackerIndex();
    }

    private StateHandler getState(TCPClient client, int trackerIndex) throws IOException, ClassNotFoundException {
        client.out().writeObject(new Message(MessageType.GET_STATE, trackerIndex));
        Message reply = ((Message) client.in().readObject());
        if (reply.getType().equals(MessageType.SEND_STATE))
            return new StateHandler(reply.getState());
        throw new IOException();
    }

    private static class IncomingMessageHandler extends Thread {
        private Socket clientSocket;
        private StateHandler state;

        public IncomingMessageHandler(Socket socket, StateHandler state) {
            this.clientSocket = socket;
            this.state = state;
        }

        @Override
        public void run() {
            try {
                TCPClient client = new TCPClient(clientSocket);
                Message inputMessage = (Message) client.in().readObject();
                switch (inputMessage.getType()) {
                    case SEND_NEW_REPLICA:
                        //TODO: Add the new replica to the state
                        break;
                    case REMOVE_OLD_REPLICA:
                        //TODO: Remove the replica from the state
                        break;
                    case GET_STATE:
                        //TODO: Send the state to the replica
                        break;
                    case READ_REPLICA:
                        //TODO: Return the correct value to the client
                        break;
                    case WRITE_REPLICA:
                        //TODO: Write the value and send an update to the other replicas
                        break;
                    default:
                        logger.log(Level.WARNING, "Message type not found.");
                }
                client.close();
                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                logger.log(Level.WARNING, "Communication with a replica interrupted.");
            }
        }
    }
}
