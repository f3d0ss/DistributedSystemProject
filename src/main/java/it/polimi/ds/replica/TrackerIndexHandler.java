package it.polimi.ds.replica;

import java.util.PriorityQueue;
import java.util.Queue;

public class TrackerIndexHandler {
    private Queue<TrackerUpdate> updateFromTrackerQueue;
    private int trackerIndex;

    public TrackerIndexHandler(int trackerIndex) {
        this.trackerIndex = trackerIndex;
        this.updateFromTrackerQueue = new PriorityQueue<>();
    }

    public synchronized int getTrackerIndex() {
        return trackerIndex;
    }

    public synchronized void executeTrackerUpdate(TrackerUpdate trackerUpdate, StateHandler state) {
        if (trackerUpdate.getTrackerIndex() > this.trackerIndex + 1){
            updateFromTrackerQueue.add(trackerUpdate);
        }else if (trackerUpdate.getTrackerIndex() == this.trackerIndex + 1) {
            if (trackerUpdate.getType().equals(TrackerUpdate.JOIN))
                state.addAddressKey(trackerUpdate.getAddress());
            else if (trackerUpdate.getType().equals(TrackerUpdate.EXIT))
                state.removeAddressKey(trackerUpdate.getAddress());
            trackerIndex++;
//            run the queues

        }
    }

    /**
     *
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

}
