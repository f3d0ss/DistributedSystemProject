package it.polimi.ds.network;

import java.io.Serializable;
import java.util.Set;

public class Message implements Serializable {
    private MessageType type;
    private Address address;
    private Set<Address> addressSet;

    public Message(MessageType type, Address address) {
        if (type.hasPayload() != MessageType.ADDRESS)
            throw new RuntimeException("This type of message shouldn't have an address");
        this.type = type;
        this.address = address;
    }

    public Message(MessageType type, Set<Address> addressSet) {
        if (type.hasPayload() != MessageType.ADDRESS_SET)
            throw new RuntimeException("This type of message shouldn't have an address set");
        this.type = type;
        this.addressSet = addressSet;
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public Address getAddress() {
        return address;
    }
}
