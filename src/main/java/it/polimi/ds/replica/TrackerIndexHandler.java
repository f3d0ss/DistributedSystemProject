package it.polimi.ds.replica;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.ReplicaState;
import it.polimi.ds.network.Update;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackerIndexHandler {
    private Set<TrackerUpdate> updateFromTrackerQueue;
    private Set<Update> updateToBeSendQueue;
    private int trackerIndex;

    public TrackerIndexHandler(int trackerIndex) {
        this.trackerIndex = trackerIndex;
        this.updateFromTrackerQueue = new HashSet<>();
        this.updateToBeSendQueue = new HashSet<>();
    }

    public synchronized int getTrackerIndex() {
        return trackerIndex;
    }

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
            for (Update update : updateToBeSendQueue) {
                updateToBeSendQueue.remove(update);
                for (Address replica : activeReplicas) {
                    Replica.addMessageToBeSent();
                    Thread writeSender = new WriteSender(replica, update, activeReplicas, trackerIndex, this);
                    writeSender.start();
                }
            }
            for (TrackerUpdate queuedTrackerUpdate : updateFromTrackerQueue) {
                updateFromTrackerQueue.remove(queuedTrackerUpdate);
                executeTrackerUpdate(queuedTrackerUpdate, state, activeReplicas);
            }
        }
    }

    /**
     * @param update
     * @param incomingTrackerIndex
     * @param state
     * @return true if Update will be handle, false if need to send `wait`
     */
    public synchronized boolean checkTrackerIndexAndExecuteUpdate(Update update, int incomingTrackerIndex, StateHandler state) {
        if (incomingTrackerIndex < this.trackerIndex)
            return false;
        state.replicaWrite(update, incomingTrackerIndex == this.trackerIndex);
        return true;

    }

    public synchronized ReplicaState checkTrackerIndexAndGetState(int incomingTrackerIndex, StateHandler stateHandler) {
        if (incomingTrackerIndex > trackerIndex)
            return null;
        return stateHandler.getState();
    }

    /**
     * Methods called if `wait` is received
     *
     * @param update
     * @param incomingTrackerIndex
     * @return true if you can resend the update
     */
    public synchronized boolean addToQueueOrRetryWrite(Update update, int incomingTrackerIndex) {
        if (this.trackerIndex > incomingTrackerIndex)
            return true;
        updateToBeSendQueue.add(update);
        return false;
    }

    public synchronized boolean isOutgoingQueueEmpty() {
        return updateToBeSendQueue.isEmpty();
    }

}
