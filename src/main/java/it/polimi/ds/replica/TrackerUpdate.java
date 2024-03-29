package it.polimi.ds.replica;

import it.polimi.ds.network.Address;

/**
 * This class represent the update sent by the tracker to the replicas
 */
public class TrackerUpdate {
    public static final String JOIN = "JOIN";
    public static final String EXIT = "EXIT";
    private final String type;
    private final Address address;
    private final int trackerIndex;

    public TrackerUpdate(String type, Address address, int trackerIndex) {
        this.type = type;
        this.address = address;
        this.trackerIndex = trackerIndex;
    }

    public String getType() {
        return type;
    }

    public Address getAddress() {
        return address;
    }

    public int getTrackerIndex() {
        return trackerIndex;
    }
}
