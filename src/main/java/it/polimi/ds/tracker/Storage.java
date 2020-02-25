package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Storage {
    private Map<String, Integer> replicas;

    public Storage() {
        this.replicas = new HashMap<>();
    }

    protected void addReplica(Address address) {
        replicas.put(address.toString(), 0);
    }

    protected void removeReplica(Address address) {
        replicas.remove(address.toString());
    }

    protected ArrayList<Address> getReplicas() {
        ArrayList<Address> addresses = new ArrayList<>();
        replicas.keySet().forEach(s -> addresses.add(Address.fromString(s)));
        return addresses;
    }

    // return the address of the replica
    protected Address addClient() {
        Map.Entry<String, Integer> min = null;
        for (Map.Entry<String, Integer> entry : replicas.entrySet()) {
            if (min == null || min.getValue() > entry.getValue()) {
                min = entry;
            }
        }
        if (min == null)
            return null;
        replicas.replace(min.getKey(), min.getValue() + 1);
        return Address.fromString(min.getKey());
    }

    protected void removeClient(Address from) {
        replicas.computeIfPresent(from.toString(), (address, integer) -> replicas.put(address, replicas.get(address) - 1));
    }
}
