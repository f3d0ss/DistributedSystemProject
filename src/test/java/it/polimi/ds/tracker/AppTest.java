package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest {
    public static final int PORT = App.TRACKER_PORT;
    /**
     * THOSE ARE NOT UNIT TEST, TO USE THEM YOU NEED TO RUN THE TRACKER FIRST
     */
    private ClientTest client;

    @BeforeEach
    public void setup() throws IOException {
        client = new ClientTest();
        client.startConnection("127.0.0.1", PORT);
    }

    @AfterEach
    public void tearDown() throws IOException {
        client.stopConnection();
    }

    @Test
    public void addReplicaTest() throws IOException, ClassNotFoundException {
        client.sendMessage(new Message(MessageType.ADD_REPLICA, new Address("123.123.123.124", 123)));
        Message message = client.readMessage();
        assertEquals(MessageType.SEND_OTHER_REPLICAS, message.getType());
        message.getAddressSet().forEach(address -> System.out.println(address.getIp() + ":" + address.getPort()));
    }

    @Test
    public void removeReplicaTest() throws IOException {
        client.sendMessage(new Message(MessageType.REMOVE_REPLICA, new Address("123.123.123.123", 123)));
    }

    @Test
    public void addClientTest() throws IOException, ClassNotFoundException {
        client.sendMessage(new Message(MessageType.ADD_CLIENT));
        Message message = client.readMessage();
        assertEquals(MessageType.SEND_REPLICA, message.getType());
        System.out.println(message.getAddress().getIp() + ":" + message.getAddress().getPort());
    }

    @Test
    public void removeClientTest() throws IOException {
        client.sendMessage(new Message(MessageType.REMOVE_CLIENT));
    }
}
