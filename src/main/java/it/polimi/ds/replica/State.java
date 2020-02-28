package it.polimi.ds.replica;

import it.polimi.ds.network.Address;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class State implements Serializable {
    private Map<String, Integer> vectorClock;
    private Map<String, String> store;

    public State(Address myAddress) {
        vectorClock = new HashMap<>();
        vectorClock.put(myAddress.toString(), 0);
        store = new HashMap<>();
    }

    public Map<String, Integer> getVectorClock() {
        return new HashMap<>(vectorClock);
    }

    public void write(Map<String, Integer> vectorClock, String key, String value){
        this.vectorClock = new HashMap<>(vectorClock);
        store.put(key, value);
    }

    public String read(String key){
        return store.get(key);
    }

    public void removeKey(String key) {
        vectorClock.remove(key);
    }

    public void addKey(String key) {
        vectorClock.putIfAbsent(key, 0);
    }
}
