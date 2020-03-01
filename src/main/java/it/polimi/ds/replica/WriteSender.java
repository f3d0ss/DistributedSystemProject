package it.polimi.ds.replica;

import it.polimi.ds.network.*;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WriteSender extends Thread {
    private Address otherReplica;
    private Update update;
    private List<Address> activeReplicas;
    private int trackerIndex;
    private TrackerIndexHandler trackerIndexHandler;
    private static final Logger logger = Logger.getLogger("WriteSender");

    /**
     *
     * @param otherReplica this is the
     * @param update this is the update to be sent
     * @param activeReplicas this is used to check other replica is removed from the list of activeReplica
     * @param trackerIndex
     * @param trackerIndexHandler
     *
     */
    public WriteSender(Address otherReplica, Update update, List<Address> activeReplicas, int trackerIndex, TrackerIndexHandler trackerIndexHandler) {
        this.otherReplica = otherReplica;
        this.update = update;
        this.activeReplicas = activeReplicas;
        this.trackerIndex = trackerIndex;
        this.trackerIndexHandler = trackerIndexHandler;
    }

    /**
     * This method continuously try to connect to otherReplica to send the update if otherReplica is still in the list of active replicas (activeReplicas)
     */
    @Override
    public void run() {
        try {
            TCPClient replica = TCPClient.connect(otherReplica);
            replica.out().writeObject(new Message(MessageType.UPDATE_FROM_REPLICA, update, trackerIndex));
            Message reply = (Message) replica.in().readObject();
            if (reply.getType() == MessageType.WAIT) {
                if (trackerIndexHandler.addToQueueOrRetryWrite(update, trackerIndex)){
                    for (Address address : activeReplicas) {
                        Replica.addMessageToBeSent();
                        Thread writeSender = new WriteSender(address, update, activeReplicas, trackerIndex, trackerIndexHandler);
                        writeSender.start();
                    }
                }
            }
            // otherwise the reply should be an ACK and nothing need to be done
            replica.close();
            Replica.removeMessageToBeSent();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, () -> "Could not update replica " + otherReplica + " properly.");
            if (activeReplicas.contains(otherReplica))
                run();
        }
    }
}
