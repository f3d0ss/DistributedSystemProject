package it.polimi.ds.network;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private MessageType type;
    private Address address;
    private List<Address> addressSet;
    private String resource;
    private String value;
    private ReplicaState state;
    private Update update;
    private int trackerIndex;

    public Message(MessageType type, Address address) {
        if (!type.hasPayload().equals(MessageType.ADDRESS))
            throw new WrongMessageException("This type of message shouldn't have an address.");
        this.type = type;
        this.address = address;
    }

    public Message(MessageType type, Address address, int trackerIndex) {
        if (!type.hasPayload().equals(MessageType.ADDRESS_TRACKER_INDEX))
            throw new WrongMessageException("This type of message shouldn't have an address or tracker index.");
        this.type = type;
        this.address = address;
        this.trackerIndex = trackerIndex;
    }

    public Message(MessageType type, List<Address> addressSet, int trackerIndex) {
        if (!type.hasPayload().equals(MessageType.ADDRESS_SET_TRACKER_INDEX))
            throw new WrongMessageException("This type of message shouldn't have an address set.");
        this.type = type;
        this.addressSet = addressSet;
        this.trackerIndex = trackerIndex;
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String resource) {
        if (!type.hasPayload().equals(MessageType.READ))
            throw new WrongMessageException("This type of message shouldn't have a resource.");
        this.type = type;
        this.resource = resource;
    }

    public Message(MessageType type, String resource, String value) {
        if (!type.hasPayload().equals(MessageType.KEY_VALUE))
            throw new WrongMessageException("This type of message shouldn't have a resource or value.");
        this.type = type;
        this.resource = resource;
        this.value = value;
    }

    public Message(MessageType type, ReplicaState state) {
        if (!type.hasPayload().equals(MessageType.STATE))
            throw new WrongMessageException("This type of message shouldn't have a state.");
        this.type = type;
        this.state = state;
    }

    public Message(MessageType type, int trackerIndex) {
        if (!type.hasPayload().equals(MessageType.TRACKER_INDEX))
            throw new WrongMessageException("This type of message shouldn't have a tracker index.");
        this.type = type;
        this.trackerIndex = trackerIndex;
    }

    public Message(MessageType type, Update update, int trackerIndex) {
        if (!type.hasPayload().equals(MessageType.UPDATE))
            throw new WrongMessageException("This type of message shouldn't have an update.");
        this.type = type;
        this.update = update;
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

    public ReplicaState getState() {
        return state;
    }

    public int getTrackerIndex() {
        return trackerIndex;
    }

    public Update getUpdate() {
        return update;
    }

    private static class WrongMessageException extends RuntimeException {
        public WrongMessageException(String message) {
            super(message);
        }
    }
}
