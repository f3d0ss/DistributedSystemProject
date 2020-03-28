package it.polimi.ds.replica;

import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.tracker.Tracker;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ReplicaTest {
    private static final String LOCALHOST = "127.0.0.1";
    private int trackerPort;
    private int replica1Port;
    private int replica2Port;
    private static Thread tracker, replica1, replica2;
    private Message answer;

    // This test run the replicas in a base case scenario with 1 tracker, 2 replicas and 2 clients
    @Test
    public void baseTest() {
        try {
            // Starting the tracker
            trackerPort = ReplicaHelper.getPort();
            tracker = new Thread(() -> Tracker.main(new String[]{Integer.toString(trackerPort)}));
            tracker.start();

            // Starting the first replica
            replica1Port = ReplicaHelper.getPort();
            replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), Integer.toString(replica1Port)}));
            replica1.start();
            Thread.sleep(100);

            // Starting the second replica
            replica2Port = ReplicaHelper.getPort();
            replica2 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), Integer.toString(replica2Port)}));
            replica2.start();
            Thread.sleep(100);

            // Starting the first client
            answer = ReplicaHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));
            assertEquals(replica1Port, answer.getAddress().getPort());

            // Starting  the second client
            answer = ReplicaHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));
            assertEquals(replica2Port, answer.getAddress().getPort());
            Thread.sleep(100);

            // Write of client1 on replica1
            ReplicaHelper.sendMessage(replica1Port, new Message(MessageType.WRITE_FROM_CLIENT, "x", "1"));

            // Read of client1 on replica1
            answer = ReplicaHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, "x"));
            assertEquals("x", answer.getResource());
            assertEquals("1", answer.getValue());

            // Write of client2 on replica2
            ReplicaHelper.sendMessage(replica2Port, new Message(MessageType.WRITE_FROM_CLIENT, "y", "2"));
            Thread.sleep(100);

            // Read of client1 of resource written by client2 (y)
            answer = ReplicaHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, "y"));
            assertEquals("y", answer.getResource());
            assertEquals("2", answer.getValue());

            // Read of client2 of resource written by client1 (x)
            answer = ReplicaHelper.sendMessageAndReceive(replica2Port, new Message(MessageType.READ_FROM_CLIENT, "x"));
            assertEquals("x", answer.getResource());
            assertEquals("1", answer.getValue());

            // Closing all replicas
            replica1.interrupt();
            replica2.interrupt();

            // Closing tracker
            tracker.interrupt();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            fail();
        }
    }
}
