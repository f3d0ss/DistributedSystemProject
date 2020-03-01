package it.polimi.ds.network;

import java.io.Serializable;
import java.util.Map;

public class Update implements Serializable {
    private Map<String, Integer> vectorClock;
    private Address from;
    private String key;
    private String value;

    public Update(Map<String, Integer> vectorClock, Address from, String key, String value) {
        this.vectorClock = vectorClock;
        this.from = from;
        this.key = key;
        this.value = value;
    }

    public Map<String, Integer> getVectorClock() {
        return vectorClock;
    }

    public Address getFrom() {
        return from;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
