package it.polimi.ds.replica;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.ReplicaState;
import it.polimi.ds.network.Update;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class handle the TrackerIndex with the needed synchronization
 * It also enqueue the update from the tracker if they come out of order
 * and enqueue the update to be send if a replica replied with wait
 */
public class TrackerIndexHandler {
    private static final Logger logger = Logger.getLogger("TrackerIndexHandler");
    private final Set<TrackerUpdate> updateFromTrackerQueue;
    private final Set<UpdateToBeSendQueueElements> updateToBeSendQueue;
    private int trackerIndex;

    public TrackerIndexHandler(int trackerIndex) {
        this.trackerIndex = trackerIndex;
        this.updateFromTrackerQueue = new HashSet<>();
        this.updateToBeSendQueue = new HashSet<>();
    }

    public synchronized int getTrackerIndex() {
        return trackerIndex;
    }

    /**
     * This method execute the update from the tracker or put it in queue if out of order
     *
     * @param trackerUpdate  update from tracker
     * @param state          the state of the replica to be update in case some update from other replica become executable
     * @param activeReplicas the list of other replica to be update
     */
    public synchronized void executeTrackerUpdate(TrackerUpdate trackerUpdate, StateHandler state, List<Address> activeReplicas) {
        if (trackerUpdate.getTrackerIndex() > this.trackerIndex + 1) {
            updateFromTrackerQueue.add(trackerUpdate);
        } else if (trackerUpdate.getTrackerIndex() == this.trackerIndex + 1) {
            if (trackerUpdate.getType().equals(TrackerUpdate.JOIN)) {
                state.addAddressKey(trackerUpdate.getAddress());
                activeReplicas.add(trackerUpdate.getAddress());
            } else if (trackerUpdate.getType().equals(TrackerUpdate.EXIT)) {
                state.removeAddressKey(trackerUpdate.getAddress());
                activeReplicas.remove(trackerUpdate.getAddress());
            }
            trackerIndex++;
            for (UpdateToBeSendQueueElements updateToBeSendQueueElement : updateToBeSendQueue) {
                List<Address> newReplicas = activeReplicas.stream().filter(address -> !updateToBeSendQueueElement.getOtherReplicasAlreadySent().contains(address)).collect(Collectors.toList());
                updateToBeSendQueueElement.getOtherReplicasAlreadySent().addAll(newReplicas);
                for (Address address : newReplicas) {
                    Replica.addMessageToBeSent();
                    Thread writeSender = new WriteSender(address, updateToBeSendQueueElement.getUpdate(), activeReplicas, this.trackerIndex, this, updateToBeSendQueueElement.getOtherReplicasAlreadySent());
                    writeSender.start();
                }

                if (updateToBeSendQueueElement.getIncomingTrackerIndex() <= this.trackerIndex) {
                    updateToBeSendQueue.remove(updateToBeSendQueueElement);
                }
            }
            for (TrackerUpdate queuedTrackerUpdate : updateFromTrackerQueue) {
                updateFromTrackerQueue.remove(queuedTrackerUpdate);
                executeTrackerUpdate(queuedTrackerUpdate, state, activeReplicas);
            }
        }
    }

    /**
     * This method execute the update on the state
     *
     * @param update               the update
     * @param incomingTrackerIndex the tracker index of the incoming update
     * @param state                the state of the replica to be update
     * @return my trackerIndex if the incoming is less then mine, 0 otherwise
     */
    public synchronized int checkTrackerIndexAndExecuteUpdate(Update update, int incomingTrackerIndex, StateHandler state) {
        logger.log(Level.INFO, () -> "Update received from: \t" + update.getFrom() + "\t with tracker index = " + incomingTrackerIndex);
        state.replicaWrite(update, incomingTrackerIndex, this.trackerIndex);
        if (incomingTrackerIndex < this.trackerIndex) {
            return this.trackerIndex;
        }
        return 0;

    }

    /**
     * This method return the state only if the incoming tracker index is less or equal to the tracker index this replica
     */
    public synchronized ReplicaState checkTrackerIndexAndGetState(int incomingTrackerIndex, StateHandler stateHandler) {
        if (incomingTrackerIndex > trackerIndex)
            return null;
        return stateHandler.getState();
    }

    /**
     * Methods called if `wait` is received
     * add the update waiting for un update of the tracker index or retry to send the update if the tracker is already changed
     *
     * @param update                  the update that has to be sent
     * @param incomingTrackerIndex    the tracker index of the wait reply
     * @param outgoingTrackerIndex    the tracker index of this replica when it tried to send the update
     * @param otherReplicasBeforeSend the list of replicas where it already sent the update
     */
    public synchronized void addToQueueOrRetryWrite(Update update, int outgoingTrackerIndex, int incomingTrackerIndex, List<Address> otherReplicasBeforeSend, List<Address> activeReplicas) {
        if (this.trackerIndex > outgoingTrackerIndex) {
            // here only if the message can be sent immediately
            // check to which client to send
            List<Address> newReplicas = activeReplicas.stream().filter(address -> !otherReplicasBeforeSend.contains(address)).collect(Collectors.toList());
            otherReplicasBeforeSend.addAll(newReplicas);
            for (Address address : newReplicas) {
                Replica.addMessageToBeSent();
                Thread writeSender = new WriteSender(address, update, activeReplicas, outgoingTrackerIndex + 1, this, otherReplicasBeforeSend);
                writeSender.start();
            }

            if (incomingTrackerIndex <= outgoingTrackerIndex + 1) {
                return;
            }
        }
        updateToBeSendQueue.add(new UpdateToBeSendQueueElements(update, otherReplicasBeforeSend, incomingTrackerIndex));
    }

    public boolean isOutgoingQueueEmpty() {
        return updateToBeSendQueue.isEmpty();
    }

    /**
     * This class represents an update that has to be sent after a tracker index update
     */
    private static class UpdateToBeSendQueueElements {
        private final Update update;
        private final List<Address> otherReplicasAlreadySent;
        private final int incomingTrackerIndex;

        public UpdateToBeSendQueueElements(Update update, List<Address> otherReplicasAlreadySent, int incomingTrackerIndex) {
            this.update = update;
            this.otherReplicasAlreadySent = otherReplicasAlreadySent;
            this.incomingTrackerIndex = incomingTrackerIndex;
        }

        public Update getUpdate() {
            return update;
        }

        public List<Address> getOtherReplicasAlreadySent() {
            return otherReplicasAlreadySent;
        }

        public int getIncomingTrackerIndex() {
            return incomingTrackerIndex;
        }
    }

}
