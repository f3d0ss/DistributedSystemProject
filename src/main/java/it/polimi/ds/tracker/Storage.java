package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Storage {
    private Map<Address, Integer> replicas;

    public Storage() {
        this.replicas = new HashMap<>();
    }

    protected void addReplica(Address address) {
        replicas.put(address, 0);
    }

    protected void removeReplica(Address address) {
        replicas.remove(address);
    }

    protected Set<Address> getReplicas() {
        return replicas.keySet();
    }

    // return the address of the replica
    protected Address addClient() {
        Map.Entry<Address, Integer> min = null;
        for (Map.Entry<Address, Integer> entry : replicas.entrySet()) {
            if (min == null || min.getValue() > entry.getValue()) {
                min = entry;
            }
        }
        if (min == null)
            return null;
        replicas.replace(min.getKey(), min.getValue() + 1);
        return min.getKey();
    }

    protected void removeClient(Address from) {
        replicas.computeIfPresent(from, (address, integer) -> replicas.put(address, replicas.get(address) - 1));
    }
}
