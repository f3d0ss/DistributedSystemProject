package it.polimi.ds.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class ReplicaState implements Serializable {
    private Map<String, Integer> vectorClock;
    private Map<String, String> store;
    private Queue<UpdateWithTracker> queue;

    public ReplicaState(Address myAddress) {
        vectorClock = new HashMap<>();
        vectorClock.put(myAddress.toString(), 0);
        store = new HashMap<>();
        this.queue = new PriorityQueue<>();
    }

    public ReplicaState(ReplicaState copyState) {
        this.vectorClock = new HashMap<>(copyState.getVectorClock());
        this.store = new HashMap<>(copyState.getStore());
        this.queue = new PriorityQueue<>(copyState.getQueue());
    }

    public Map<String, Integer> getVectorClock() {
        return new HashMap<>(vectorClock);
    }

    public void write(Map<String, Integer> vectorClock, String key, String value) {
        this.vectorClock = new HashMap<>(vectorClock);
        store.put(key, value);
    }

    public Queue<UpdateWithTracker> getQueue() {
        return queue;
    }

    public String read(String key) {
        return store.get(key);
    }

    public void removeKey(String key) {
        vectorClock.remove(key);
    }

    public void addKey(String key) {
        vectorClock.putIfAbsent(key, 0);
    }

    private Map<String, String> getStore() {
        return store;
    }
}
