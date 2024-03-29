package it.polimi.ds.network;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the Address (IP and Port) needed for communication among entities.
 */
public class Address implements Serializable {
    private final String ip;
    private final Integer port;

    public Address(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public static Address fromString(String address) {
        String[] addressSplit = address.split(":");
        return new Address(addressSplit[0], Integer.parseInt(addressSplit[1]));
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public String toString() {
        return ip + ":" + port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Address)
            return ((Address) obj).getIp().equals(ip) && ((Address) obj).getPort().equals(port);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
