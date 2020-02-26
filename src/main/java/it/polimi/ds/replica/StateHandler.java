package it.polimi.ds.replica;

import it.polimi.ds.network.Address;

import java.util.Map;
import java.util.Queue;

// This class exist to synchronize the access to the State (TODO)
public class StateHandler {
    private State state;
    private Queue<Update> queue;

    public StateHandler(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String read(String key){
        return state.read(key);
    }

    public void clientWrite(Address replicaAddress, String key, String value){
        Map<String, Integer> newVector = state.getVectorClock();
        newVector.put(replicaAddress.toString(), newVector.get(replicaAddress.toString()) + 1);
        state.write(newVector, key, value);
    }

    public void replicaWrite(Map<String, Integer> updateVectorClock, Address from, String key, String value){
        Map<String, Integer> myVector = state.getVectorClock();
        if (vectorCheck(myVector, updateVectorClock, from)){
            myVector.put(from.toString(), myVector.get(from.toString()) + 1); // myVector[from] ++
            state.write(myVector, key, value);
            checkUpdateQueue();
        }else {
            queue.add(new Update(updateVectorClock, from, key, value));
        }
    }

    private void checkUpdateQueue(){
        Map<String, Integer> myVector = state.getVectorClock();
        for (Update update: queue){
            if (vectorCheck(myVector, update.getVectorClock(), update.getFrom())){
                queue.remove(update);
                myVector.put(update.getFrom().toString(), myVector.get(update.getFrom().toString()) + 1); // myVector[from] ++
                state.write(myVector, update.getKey(), update.getValue());
                checkUpdateQueue();
                break;
            }
        }
    }

    private static boolean vectorCheck(Map<String, Integer> oldVector, Map<String, Integer> newVector, Address from){
        for (Map.Entry<String, Integer> entry : newVector.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            if (key.equals(from.toString()) && value != oldVector.getOrDefault(key, 0) + 1)
                return false;
            if (value > oldVector.getOrDefault(key, 0))
                return false;
        }
        return true;
    }

    private static class Update{
        private Map<String, Integer> vectorClock;
        private Address from;
        private String key;
        private String value;

        public Update(Map<String, Integer> vectorClock, Address from, String key, String value) {
            this.vectorClock = vectorClock;
            this.from = from;
            this.key = key;
            this.value = value;
        }

        public Map<String, Integer> getVectorClock() {
            return vectorClock;
        }

        public Address getFrom() {
            return from;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
