package it.polimi.ds.replica;

import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.SimulateDelay;
import it.polimi.ds.tracker.Tracker;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class DelayReplicaTest {
    private static final String LOCALHOST = "127.0.0.1";
    private static final String minDelay = "400";
    private static final String maxDelay = "900";
    private static Thread tracker, replica1, replica2;
    private int trackerPort;
    private int replica1Port;
    private int replica2Port;
    private Message answer;

    @Test
    public void baseTest() {
        try {
            // Starting the tracker
            trackerPort = ReplicaTestHelper.getPort();
            tracker = new Thread(() -> Tracker.main(new String[]{Integer.toString(trackerPort), minDelay, maxDelay}));
            tracker.start();

            // Starting the first replica
            replica1Port = ReplicaTestHelper.getPort();
            replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(replica1Port), minDelay, maxDelay}));
            replica1.start();
            SimulateDelay.fixed(1000);

            // Starting the second replica
            replica2Port = ReplicaTestHelper.getPort();
            replica2 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(replica2Port), minDelay, maxDelay}));
            replica2.start();
            SimulateDelay.fixed(1000);

            // Starting the first client
            answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));
            SimulateDelay.fixed(1000);

            // Starting  the second client
            answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));
            SimulateDelay.fixed(1000);

            // Write of client1 on replica1
            ReplicaTestHelper.sendMessage(replica1Port, new Message(MessageType.WRITE_FROM_CLIENT, "x", "1"));

            // Write of client2 on replica2
            ReplicaTestHelper.sendMessage(replica2Port, new Message(MessageType.WRITE_FROM_CLIENT, "x", "2"));

            SimulateDelay.fixed(1000);

            // Read of client1 of resource written by client2 (x)
            answer = ReplicaTestHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, "x"));
            assertEquals("x", answer.getResource());
            assertEquals("2", answer.getValue());

            // Read of client2 of resource written by client1 (x)
            answer = ReplicaTestHelper.sendMessageAndReceive(replica2Port, new Message(MessageType.READ_FROM_CLIENT, "x"));
            assertEquals("x", answer.getResource());
            assertEquals("1", answer.getValue());

            // Closing all replicas and tracker
            replica1.interrupt();
            replica2.interrupt();
            tracker.interrupt();
        } catch (IOException | ClassNotFoundException e) {
            fail();
        }
    }
}
