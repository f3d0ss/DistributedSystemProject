package it.polimi.ds.replica;

import it.polimi.ds.network.Address;

import java.io.Serializable;
import java.util.HashMap;

public class State implements Serializable {
    private HashMap<String, Integer> vectorClock;
    private HashMap<String, String> store;

    public State(Address myAddress) {
        vectorClock = new HashMap<>();
        vectorClock.put(myAddress.toString(), 0);
        store = new HashMap<>();

    }

    public HashMap<String, Integer> getVectorClock() {
        return new HashMap<>(vectorClock);
    }

    public void write(HashMap<String, Integer> vectorClock, String key, String value){
        this.vectorClock = new HashMap<>(vectorClock);
        store.put(key, value);
    }

    public String read(String key){
        return store.get(key);
    }


}
