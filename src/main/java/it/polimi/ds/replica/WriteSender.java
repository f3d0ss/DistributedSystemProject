package it.polimi.ds.replica;

import it.polimi.ds.network.*;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WriteSender extends Thread {
    private static final Logger logger = Logger.getLogger("WriteSender");
    private final Address otherReplica;
    private final Update update;
    private final List<Address> activeReplicas;
    private final List<Address> otherReplicasBeforeSend;
    private final int outgoingTrackerIndex;
    private final TrackerIndexHandler trackerIndexHandler;

    /**
     * @param otherReplica            this is the
     * @param update                  this is the update to be sent
     * @param activeReplicas          this is used to check other replica is removed from the list of activeReplica
     * @param outgoingTrackerIndex
     * @param otherReplicasBeforeSend
     * @param trackerIndexHandler
     */
    public WriteSender(Address otherReplica, Update update, List<Address> activeReplicas, int outgoingTrackerIndex, TrackerIndexHandler trackerIndexHandler, List<Address> otherReplicasBeforeSend) {
        this.otherReplica = otherReplica;
        this.update = update;
        this.activeReplicas = activeReplicas;
        this.otherReplicasBeforeSend = otherReplicasBeforeSend;
        this.outgoingTrackerIndex = outgoingTrackerIndex;
        this.trackerIndexHandler = trackerIndexHandler;
    }

    /**
     * This method continuously try to connect to otherReplica to send the update if otherReplica is still in the list of active replicas (activeReplicas)
     */
    @Override
    public void run() {
        try {
            SimulateDelay.uniform(Replica.minDelay, Replica.maxDelay);
            TCPClient replica = TCPClient.connect(otherReplica);
            replica.out().writeObject(new Message(MessageType.UPDATE_FROM_REPLICA, update, outgoingTrackerIndex));
            Message reply = (Message) replica.in().readObject();
            if (reply.getType() == MessageType.WAIT) {
                trackerIndexHandler.addToQueueOrRetryWrite(update, outgoingTrackerIndex, reply.getTrackerIndex(), otherReplicasBeforeSend, activeReplicas);
            }
            // otherwise the reply should be an ACK and nothing need to be done
            replica.close();
            Replica.removeMessageToBeSent();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, () -> "Could not update replica " + otherReplica + " properly.");
            if (activeReplicas.contains(otherReplica)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                run();
            }
        }
    }
}
