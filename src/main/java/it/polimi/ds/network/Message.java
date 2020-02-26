package it.polimi.ds.network;

import it.polimi.ds.replica.State;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private MessageType type;
    private Address address;
    private List<Address> addressSet;
    private String resource;
    private String value;
    private State state;
    private int trackerIndex;

    public Message(MessageType type, Address address) {
        if (!type.hasPayload().equals(MessageType.ADDRESS))
            throw new RuntimeException("This type of message shouldn't have an address");
        this.type = type;
        this.address = address;
    }

    public Message(MessageType type, List<Address> addressSet) {
        if (!type.hasPayload().equals(MessageType.ADDRESS_SET))
            throw new RuntimeException("This type of message shouldn't have an address set");
        this.type = type;
        this.addressSet = addressSet;
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String resource) {
        if(!type.hasPayload().equals(MessageType.WRITE))
            throw new RuntimeException("This type of message shouldn't have a value");
        this.type = type;
        this.resource = resource;
    }

    public Message(MessageType type, String resource, String value) {
        if(!type.hasPayload().equals(MessageType.WRITE))
            throw new RuntimeException("This type of message shouldn't have a resource or value");
        this.type = type;
        this.resource = resource;
        this.value = value;
    }

    public Message(MessageType type, State state) {
        if(!type.hasPayload().equals(MessageType.STATE))
            throw new RuntimeException("This type of message shouldn't have a state");
        this.type = type;
        this.state = state;
    }

    public Message(MessageType type, int trackerIndex) {
        if(!type.hasPayload().equals(MessageType.TRACKER_INDEX))
            throw new RuntimeException("This type of message shouldn't have a tracker index");
        this.type = type;
        this.trackerIndex = trackerIndex;
    }

    public MessageType getType() {
        return type;
    }

    public Address getAddress() {
        return address;
    }

    public List<Address> getAddressSet() {
        return addressSet;
    }

    public String getResource() {
        return resource;
    }

    public String getValue() {
        return value;
    }

    public State getState() {
        return state;
    }

    public int getTrackerIndex() {
        return trackerIndex;
    }
}
