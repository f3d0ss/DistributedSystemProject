package it.polimi.ds.replica;


import it.polimi.ds.network.Address;

public class TrackerUpdate {
    public static final String JOIN = "JOIN";
    public static final String EXIT = "EXIT";
    private String type;
    private Address address;
    private int trackerIndex;

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