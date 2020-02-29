package it.polimi.ds.replica;

import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;
import it.polimi.ds.tracker.Tracker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ReplicaTest {
    public static final String LOCALHOST = "127.0.0.1";
    public static final int TRACKER_PORT = 2222;
    public static final int REPLICA1_PORT = 3333;
    public static final int REPLICA2_PORT = 4444;
    private static Thread tracker, replica1, replica2;
    private TCPClient client1, client2;
    private Message inputMessage;

    @BeforeAll
    public static void startTracker() {
        // Starting the tracker
        tracker = new Thread(() -> Tracker.main(new String[]{Integer.toString(TRACKER_PORT)}));
        tracker.start();
    }

    @AfterAll
    public static void interruptTracker() {
        // Closing tracker
        tracker.interrupt();
    }

    @Test
    public void test1() {
        try {
            // Starting the first replica
            replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(TRACKER_PORT), Integer.toString(REPLICA1_PORT)}));
            replica1.start();
            Thread.sleep(100);

            // Starting the second replica TODO: Un-comment after the implementation of the handling of multiple replicas
            //replica2 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(TRACKER_PORT), Integer.toString(REPLICA2_PORT)}));
            //replica2.start();
            //Thread.sleep(100);

            // Starting the first client
            client1 = TCPClient.connect(LOCALHOST, TRACKER_PORT);
            client1.out().writeObject(new Message(MessageType.ADD_CLIENT));
            inputMessage = (Message) client1.in().readObject();
            assertEquals(REPLICA1_PORT, inputMessage.getAddress().getPort());
            client1.close();

            // Starting  the second client TODO: Un-comment after the implementation of the handling of multiple replicas
            //client2 = TCPClient.connect(LOCALHOST, TRACKER_PORT);
            //client2.out().writeObject(new Message(MessageType.ADD_CLIENT));
            //inputMessage = (Message) client2.in().readObject();
            //assertEquals(REPLICA2_PORT, inputMessage.getAddress().getPort());
            //client2.close();
            //Thread.sleep(100);

            // Write of client1 on replica1
            client1 = TCPClient.connect(LOCALHOST, REPLICA1_PORT);
            client1.out().writeObject(new Message(MessageType.WRITE_FROM_CLIENT, "x", "1"));
            client1.close();

            // Read of client1 on replica1
            client1 = TCPClient.connect(LOCALHOST, REPLICA1_PORT);
            client1.out().writeObject(new Message(MessageType.READ_FROM_CLIENT, "x"));
            inputMessage = (Message) client1.in().readObject();
            assertEquals("x", inputMessage.getResource());
            assertEquals("1", inputMessage.getValue());
            client1.close();

            // Closing all replicas
            replica1.interrupt();
            //replica2.interrupt();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            fail();
        }
    }
}
