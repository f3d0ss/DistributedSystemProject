package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageTest {
    private Storage storage = new Storage();

    @Test
    public void testAddReplica(){
        for (int i = 0; i < 10; i++) {
            storage.addReplica(new Address("0.0.0." + i, i));
        }
        ArrayList<Address> addresses = storage.getReplicas();
        for (int i = 0; i < 10; i++) {
            assertTrue(addresses.contains(new Address("0.0.0." + i, i)));
        }
        for (int i = 0; i < 10; i++) {
            storage.removeReplica(new Address("0.0.0." + i, i));
        }
        assertTrue(storage.getReplicas().isEmpty());
    }
}