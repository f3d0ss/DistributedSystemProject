package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Contains all the data needed by the Tracker in order to work
 */
public class Storage extends ReentrantLock {
    private final Map<String, Integer> replicas;
    private final AtomicInteger trackerIndex = new AtomicInteger(0);

    public Storage() {
        this.replicas = new HashMap<>();
    }

    protected void addReplica(Address address) {
        replicas.put(address.toString(), 0);
    }

    protected void removeReplica(Address address) {
        replicas.remove(address.toString());
    }

    protected List<Address> getReplicas() {
        List<Address> addresses = new ArrayList<>();
        replicas.keySet().forEach(s -> addresses.add(Address.fromString(s)));
        return addresses;
    }

    /**
     * Assigned the new client to the Replica serving the least amount of Clients
     * @return the Address of the Replica that will be assigned to the Client
     */
    protected Address addClient() {
        Map.Entry<String, Integer> min = null;
        for (Map.Entry<String, Integer> entry : replicas.entrySet()) {
            if (min == null || min.getValue() > entry.getValue())
                min = entry;
        }
        if (min == null)
            return null;
        replicas.replace(min.getKey(), min.getValue() + 1);
        return Address.fromString(min.getKey());
    }

    protected void removeClient(Address from) {
        replicas.computeIfPresent(from.toString(), (address, integer) -> replicas.put(address, replicas.get(address) - 1));
    }

    public int incrementAndGetTrackerIndex() {
        return trackerIndex.incrementAndGet();
    }
}
