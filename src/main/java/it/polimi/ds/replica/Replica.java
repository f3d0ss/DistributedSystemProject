package it.polimi.ds.replica;

import it.polimi.ds.network.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Replica {
    private static final Logger logger = Logger.getLogger("Replica");
    private static final AtomicInteger messagesLeftToSend = new AtomicInteger(0);
    private static AtomicBoolean isReplicaClosing = new AtomicBoolean(false);
    private Address replicaAddress;
    private List<Address> otherReplicaAddresses;
    private StateHandler state;
    private ServerSocket serverSocket;
    private TrackerIndexHandler trackerIndexHandler;     //need to be shared

    public static void main(String[] args) {
        Replica tracker = new Replica();
        tracker.start(args[0], args[1], args[2]);
    }

    private static int getChoice() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return Integer.parseInt(reader.readLine());
        } catch (NumberFormatException | IOException e) {
            return -1;
        }
    }

    public static void addMessageToBeSent() {
        synchronized (messagesLeftToSend) {
            messagesLeftToSend.incrementAndGet();
        }
    }

    public static void removeMessageToBeSent() {
        synchronized (messagesLeftToSend) {
            messagesLeftToSend.decrementAndGet();
        }
    }

    public static boolean replicaIsNotClosing() {
        return !isReplicaClosing.get();
    }

    public static void setIsReplicaClosing() {
        Replica.isReplicaClosing.set(true);
    }

    public void start(String trackerIp, String trackerPort, String replicaPort) {
        Address trackerAddress = new Address(trackerIp, Integer.valueOf(trackerPort));
        try {
            this.replicaAddress = new Address(InetAddress.getLocalHost().getHostAddress(), Integer.valueOf(replicaPort));
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Could not start replica.");
        }
        while (trackerIndexHandler == null) {
            try {
                trackerIndexHandler = joinNetwork(TCPClient.connect(trackerAddress));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Impossible to contact the tracker, retrying.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Could not read message properly.");
                return;
            }
        }
        logger.log(Level.INFO, "Connected to the tracker successfully.");
//        Try to get the state from one of the replicas
        if (otherReplicaAddresses.isEmpty())
            state = new StateHandler(new ReplicaState(replicaAddress), replicaAddress);

        for (int i = 0; state == null; i++) {
            Address otherReplica = otherReplicaAddresses.get(i % otherReplicaAddresses.size());
            try {
                state = getState(TCPClient.connect(otherReplica), trackerIndexHandler.getTrackerIndex());
            } catch (IOException | ClassNotFoundException e) {
                logger.log(Level.WARNING, () -> "Impossible to get a valid state from " + otherReplicaAddresses + ", trying an other one.");
            }
        }

//        Here I have the state
        Thread replica = new Thread(() -> runReplica(replicaPort));
        replica.start();
        do {
            logger.log(Level.INFO, "Press 1 to close the Replica.");
        }
        while (getChoice() != 1);
        logger.log(Level.INFO, "Waiting until all messages are sent...");
        Replica.setIsReplicaClosing(); // This ensures that the replica can no longer accept incoming requests from clients

        while (true) {
            synchronized (messagesLeftToSend) {
                if (messagesLeftToSend.get() == 0 && trackerIndexHandler.isOutgoingQueueEmpty()) {
                    //Replica can exit:
                    exitNetwork(trackerAddress);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Could not close the replica properly.");
                    }
                    replica.interrupt();
                    logger.log(Level.INFO, "This replica has correctly been closed.");
                    return;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Could not send all the messages to other replica properly.");
                Thread.currentThread().interrupt();
            }
        }
    }

    private void exitNetwork(Address trackerAddress) {
        try {
            TCPClient tracker = TCPClient.connect(trackerAddress);
            tracker.out().writeObject(new Message(MessageType.REMOVE_REPLICA, replicaAddress));
            tracker.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not inform the tracker of the replica closure.");
            exitNetwork(trackerAddress);
        }
    }

    private void runReplica(String replicaPort) {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(replicaPort));
            while (true) {
                new IncomingMessageHandler(otherReplicaAddresses, serverSocket.accept(), state, trackerIndexHandler).start();
            }
        } catch (IOException e) {
            // This exception must be ignored, it happens when the main thread interrupts this one
        }
        stop();
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not close tracker properly.");
        }
    }

    private TrackerIndexHandler joinNetwork(TCPClient client) throws IOException, ClassNotFoundException {
        client.out().writeObject(new Message(MessageType.ADD_REPLICA, replicaAddress));
        Message reply = (Message) client.in().readObject();
        client.close();
        otherReplicaAddresses = reply.getAddressSet();
        return new TrackerIndexHandler(reply.getTrackerIndex());
    }

    private StateHandler getState(TCPClient client, int trackerIndex) throws IOException, ClassNotFoundException {
        client.out().writeObject(new Message(MessageType.GET_STATE, trackerIndex));
        Message reply = ((Message) client.in().readObject());
        client.close();
        if (reply.getType().equals(MessageType.SEND_STATE))
            return new StateHandler(reply.getState(), replicaAddress);
        throw new IOException();
    }

    private static class IncomingMessageHandler extends Thread {
        private List<Address> otherReplicaAddresses;
        private Socket clientSocket;
        private StateHandler state;
        private TrackerIndexHandler trackerIndexHandler;

        public IncomingMessageHandler(List<Address> otherReplicaAddresses, Socket socket, StateHandler state, TrackerIndexHandler trackerIndexHandler) {
            this.otherReplicaAddresses = new ArrayList<>(otherReplicaAddresses);
            this.clientSocket = socket;
            this.state = state;
            this.otherReplicaAddresses = otherReplicaAddresses;
            this.trackerIndexHandler = trackerIndexHandler;
        }

        @Override
        public void run() {
            try {
                TCPClient client = new TCPClient(clientSocket);
                Message inputMessage = (Message) client.in().readObject();
                switch (inputMessage.getType()) {
                    case READ_FROM_CLIENT:
                        if (Replica.replicaIsNotClosing())
                            client.out().writeObject(readFromClient(inputMessage.getResource()));
                        else
                            client.out().writeObject(new Message(MessageType.READ_ANSWER, null, null));
                        break;
                    case WRITE_FROM_CLIENT:
                        if (Replica.replicaIsNotClosing()) {
                            writeFromClient(inputMessage.getResource(), inputMessage.getValue());
                            client.out().writeObject(new Message(MessageType.ACK));
                        } else
                            client.out().writeObject(new Message(MessageType.WAIT));
                        break;
                    case UPDATE_FROM_REPLICA:
                        int trackerIndex = updateFromReplica(inputMessage.getUpdate(), inputMessage.getTrackerIndex());
                        if (trackerIndex == 0)
                            client.out().writeObject(new Message(MessageType.ACK));
                        else
                            client.out().writeObject(new Message(MessageType.WAIT, trackerIndex));
                        break;
                    case GET_STATE:
                        ReplicaState outgoingState = getReplicaState(inputMessage.getTrackerIndex(), state);
                        if (outgoingState == null)
                            client.out().writeObject(new Message(MessageType.NOT_STATE));
                        else
                            client.out().writeObject(new Message(MessageType.SEND_STATE, outgoingState));
                        break;
                    case SEND_NEW_REPLICA:
                        addNewReplica(inputMessage.getAddress(), inputMessage.getTrackerIndex(), state, otherReplicaAddresses);
                        break;
                    case REMOVE_OLD_REPLICA:
                        removeOldReplica(inputMessage.getAddress(), inputMessage.getTrackerIndex(), state, otherReplicaAddresses);
                        break;
                    default:
                        logger.log(Level.WARNING, "Message type not found.");
                }
                client.close();
                clientSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Communication with a replica interrupted.");
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Could not read the message properly.");
            }
        }

        private Message readFromClient(String resource) {
            return new Message(MessageType.READ_ANSWER, resource, state.read(resource));
        }

        private void writeFromClient(String resource, String value) {
            Update update = state.clientWrite(resource, value);
            logger.log(Level.INFO, () -> "Successfully wrote resource " + resource + " with value " + value);
            // Get indexTracker (because not send to new replicas)
            int trackerIndex = trackerIndexHandler.getTrackerIndex();
            List<Address> otherReplicaBeforeSend = new ArrayList<>(otherReplicaAddresses);
/*          here after reading the trackerIndex a thread could increment it and update the otherReplicaAddress,
            we don't care, because if it was updated by an Exit from another replica it'ok if we don't send the update to the exited replica (would be check later otherwise)
            if it was updated by a Join we will simply send the update to the new replica who will reply with `wait` causing the resend of the message, no biggy
 */
            Replica.addMessageToBeSent();
            for (Address address : otherReplicaAddresses) {
                Replica.addMessageToBeSent();
                Thread writeSender = new WriteSender(address, update, otherReplicaAddresses, trackerIndex, trackerIndexHandler, otherReplicaBeforeSend);
                writeSender.start();
            }
            Replica.removeMessageToBeSent();
        }


        /**
         * @param update
         * @param incomingTrackerIndex
         * @return my trackerIndex if the incoming is less then mine, 0 otherwise
         */
        private int updateFromReplica(Update update, int incomingTrackerIndex) {
            /*  Check the incoming trackerIndex ITI, if:
                ITI > my trackerIndex MTI then execute considering that sender know more than me
                ITI < MTI then reply with `wait` message and execute knowing that the sender know less than me
                ITI = MTI then process then execute state.replicaWrite
                NOTE: check ITI = MTI and execute state.replicaWrite should be atomic, otherwise after the check and before the replicaWrite
                the replica could receive a Join from the Tracker and give his state to the neo joined Replica
                and then process the update without replying with `wait`*/
            return trackerIndexHandler.checkTrackerIndexAndExecuteUpdate(update, incomingTrackerIndex, state);
        }

        private ReplicaState getReplicaState(int incomingTrackerIndex, StateHandler state) {
            /*  Check the incoming trackerIndex ITI, if:
                ITI > my trackerIndex MTI reply with `NOT_STATE`
                ITI < MTI then should be ok to send the state
                ITI = MTI Send the whole state to the requesting replica.
                Note: send state and queue
                */
            return trackerIndexHandler.checkTrackerIndexAndGetState(incomingTrackerIndex, state);

        }

        private void addNewReplica(Address address, int trackerIndex, StateHandler state, List<Address> activeReplicas) {
            //Use trackerIndexHandler.executeTrackerUpdate
            /*  Check the incoming trackerIndex ITI, if:
                ITI > my trackerIndex MTI + 1 then put the message in `updates from tracker waiting for T` queue
                ITI < MTI + 1 message already received, ignore
                ITI = MTI + 1 then add the Replica to the VClock and update MTI
                Note: the update of the MTI should cause the checking of the `updates from tracker waiting for T` and  `updates from replicas waiting for T` queues */
            trackerIndexHandler.executeTrackerUpdate(new TrackerUpdate(TrackerUpdate.JOIN, address, trackerIndex), state, activeReplicas);
        }

        private void removeOldReplica(Address address, int trackerIndex, StateHandler state, List<Address> activeReplicas) {
            //Use trackerIndexHandler.executeTrackerUpdate
            /*  Check the incoming trackerIndex ITI, if:
                ITI > my trackerIndex MTI + 1 then put the message in `updates from tracker waiting for T` queue
                ITI < MTI + 1 message already received, ignore
                ITI = MTI + 1 then remove the Replica from the VClock and update MTI
                Note: the update of the MTI should cause the checking of the `updates from tracker waiting for T` and  `updates from replicas waiting for T` queues */
            trackerIndexHandler.executeTrackerUpdate(new TrackerUpdate(TrackerUpdate.EXIT, address, trackerIndex), state, activeReplicas);
        }
    }
}
