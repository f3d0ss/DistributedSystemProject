package it.polimi.ds.replica;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.ReplicaState;
import it.polimi.ds.network.Update;
import it.polimi.ds.network.UpdateWithTracker;

import java.util.Map;

// This class exist to synchronize the access to the State (TODO)
public class StateHandler {
    public static final int DISCARD = -1;
    public static final int ADD_TO_QUEUE = 0;
    public static final int ACCEPT = 1;

    private ReplicaState state;

    private Address replicaAddress;

    public StateHandler(ReplicaState state, Address replicaAddress) {
        this.state = state;
        this.replicaAddress = replicaAddress;
    }

    public ReplicaState getState() {
        return new ReplicaState(state);
    }

    public void setState(ReplicaState state) {
        this.state = state;
    }

    public String read(String key){
        return state.read(key);
    }

    public synchronized void removeAddressKey(Address address){
        state.removeKey(address.toString());
    }

    public synchronized void addAddressKey(Address address){
        state.addKey(address.toString());
    }

    public synchronized Update clientWrite(String key, String value){
        Map<String, Integer> newVector = state.getVectorClock();
        newVector.put(replicaAddress.toString(), newVector.get(replicaAddress.toString()) + 1);
        state.write(newVector, key, value);
        return new Update(newVector, replicaAddress, key, value);
    }

    public synchronized void replicaWrite(Update update, boolean sameTrackerIndex){
        Map<String, Integer> myVector = state.getVectorClock();
        int check = vectorCheck(myVector, update.getVectorClock(), update.getFrom(), sameTrackerIndex);
        if (check == ACCEPT){
            myVector.put(update.getFrom().toString(), myVector.get(update.getFrom().toString()) + 1); // myVector[from] ++
            state.write(myVector, update.getKey(), update.getValue());
            checkUpdateQueue();
        }else if(check == ADD_TO_QUEUE) {
            state.getQueue().add(new UpdateWithTracker(update, sameTrackerIndex));
        }
    }

    private void checkUpdateQueue(){
        Map<String, Integer> myVector = state.getVectorClock();
        for (UpdateWithTracker updateWithTracker: state.getQueue()){
            Update update = updateWithTracker.getUpdate();
            if (vectorCheck(myVector, update.getVectorClock(), update.getFrom(), updateWithTracker.isSameTrackerIndex()) == ACCEPT){
                state.getQueue().remove(updateWithTracker);
                myVector.put(update.getFrom().toString(), myVector.get(update.getFrom().toString()) + 1); // myVector[from] ++
                state.write(myVector, update.getKey(), update.getValue());
                checkUpdateQueue();
                break;
            }
        }
    }

    private static int vectorCheck(Map<String, Integer> myVector, Map<String, Integer> newVector, Address from, boolean sameTrackerIndex){
        for (Map.Entry<String, Integer> entry : newVector.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            if (key.equals(from.toString())){
                if(value < myVector.getOrDefault(key, 0) + 1)
                    return DISCARD;
                else if (value > myVector.getOrDefault(key, 0) + 1)
                    return ADD_TO_QUEUE;
            }
            if (sameTrackerIndex){
//                here if I don't have key => key exited the network and therefore I have all his update
                if (myVector.containsKey(key) && value > myVector.get(key))
                    return ADD_TO_QUEUE;
            } else {
//                here if I don't have key => key joined the network and therefore I consider it 0
                if (value > myVector.getOrDefault(key, 0))
                    return ADD_TO_QUEUE;
            }
        }
        return ACCEPT;
    }

}
