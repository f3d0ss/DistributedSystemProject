package it.polimi.ds.tracker;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrackerTest {
    public static final int PORT = Tracker.TRACKER_PORT;
    private static Thread tracker;
    private TCPClient client;

    @BeforeAll
    public static void startTracker() {
        tracker = new Thread(() -> Tracker.main(null));
        tracker.start();
    }

    @AfterAll
    public static void interruptTracker() {
        tracker.interrupt();
    }

    @BeforeEach
    public void setup() throws IOException {
        client = TCPClient.connect("127.0.0.1", PORT);
    }

    @AfterEach
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void addReplicaTest() throws IOException, ClassNotFoundException {
        client.out().writeObject(new Message(MessageType.ADD_REPLICA, new Address("123.123.123.123", 123)));
        Message message = (Message) client.in().readObject();
        assertEquals(MessageType.SEND_OTHER_REPLICAS, message.getType());
        message.getAddressSet().forEach(address -> System.out.println(address.getIp() + ":" + address.getPort()));
    }

    @Test
    public void removeReplicaTest() throws IOException {
        client.out().writeObject(new Message(MessageType.REMOVE_REPLICA, new Address("123.123.123.123", 123)));
    }

    @Test
    public void addClientTest() throws IOException, ClassNotFoundException {
        client.out().writeObject(new Message(MessageType.ADD_REPLICA, new Address("123.123.123.123", 123)));
        Message message = (Message) client.in().readObject();
        assertEquals(MessageType.SEND_OTHER_REPLICAS, message.getType());
        TCPClient client2 = TCPClient.connect("127.0.0.1", PORT);
        client2.out().writeObject(new Message(MessageType.ADD_CLIENT));
        message = (Message) client2.in().readObject();
        assertEquals(MessageType.SEND_REPLICA, message.getType());
        System.out.println(message.getAddress().getIp() + ":" + message.getAddress().getPort());
    }

    @Test
    public void removeClientTest() throws IOException {
        client.out().writeObject(new Message(MessageType.REMOVE_CLIENT, new Address("123.123.123.123", 123)));
    }
}
