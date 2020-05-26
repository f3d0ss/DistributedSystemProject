package it.polimi.ds.network;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the update exchanged between replicas
 */
public class Update implements Serializable, Comparable<Object> {
    private final Map<String, Integer> vectorClock;
    private final Address from;
    private final String key;
    private final String value;

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

    @Override
    public int compareTo(Object o) {
        if (o instanceof Update) {
            Update c = (Update) o;
            if (from.equals(c.from)) {
                if (vectorClock.get(from.toString()) > c.vectorClock.get(c.from.toString()))
                    return 1;
                else
                    return -1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Update)
            return ((Update) obj).getVectorClock().equals(vectorClock) && ((Update) obj).getFrom().equals(from)
                    && ((Update) obj).getKey().equals(key) && ((Update) obj).getValue().equals(value);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vectorClock, from, key, value);
    }
}
