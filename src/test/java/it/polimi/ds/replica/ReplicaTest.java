package it.polimi.ds.replica;

import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.SimulateDelay;
import it.polimi.ds.tracker.Tracker;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ReplicaTest {
    private static final String LOCALHOST = "127.0.0.1";
    private static final int N = 5;
    private static Thread tracker, replica1, replica2;
    private int trackerPort;
    private int replica1Port;
    private int replica2Port;
    private Message answer;

    // This test runs the replicas in a base case scenario with 1 tracker, 2 replicas and 2 clients
    @Test
    public void baseTest() {
        try {
            // Starting the tracker
            trackerPort = ReplicaTestHelper.getPort();
            tracker = new Thread(() -> Tracker.main(new String[]{Integer.toString(trackerPort)}));
            tracker.start();

            // Starting the first replica
            replica1Port = ReplicaTestHelper.getPort();
            replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(replica1Port)}));
            replica1.start();
            SimulateDelay.fixed(100);

            // Starting the second replica
            replica2Port = ReplicaTestHelper.getPort();
            replica2 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(replica2Port)}));
            replica2.start();
            SimulateDelay.fixed(100);

            // Starting the first client
            answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));

            // Starting  the second client
            answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));
            SimulateDelay.fixed(100);

            // Write of client1 on replica1
            ReplicaTestHelper.sendMessage(replica1Port, new Message(MessageType.WRITE_FROM_CLIENT, "x", "1"));
            SimulateDelay.fixed(100);

            // Read of client1 on replica1
            answer = ReplicaTestHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, "x"));
            assertEquals("x", answer.getResource());
            assertEquals("1", answer.getValue());

            // Write of client2 on replica2
            ReplicaTestHelper.sendMessage(replica2Port, new Message(MessageType.WRITE_FROM_CLIENT, "y", "2"));
            SimulateDelay.fixed(500);

            // Read of client1 of resource written by client2 (y)
            answer = ReplicaTestHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, "y"));
            assertEquals("y", answer.getResource());
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

    // This test runs a lot of clients with only 1 tracker and 1 replica
    @Test
    public void clientSpam() {
        try {
            // Starting the tracker
            trackerPort = ReplicaTestHelper.getPort();
            tracker = new Thread(() -> Tracker.main(new String[]{Integer.toString(trackerPort)}));
            tracker.start();

            // Starting the replica
            replica1Port = ReplicaTestHelper.getPort();
            replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(replica1Port)}));
            replica1.start();
            SimulateDelay.fixed(1000);

            // Starting the first client
            int tempValue = ReplicaTestHelper.getPort();
            answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));
            ReplicaTestHelper.sendMessage(replica1Port, new Message(MessageType.WRITE_FROM_CLIENT, Integer.toString(tempValue), Integer.toString(tempValue - 1)));

            // Starting N other clients
            for (int i = 0; i < N; i++) {
                // Connecting
                tempValue = ReplicaTestHelper.getPort();
                answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));

                // Writing
                ReplicaTestHelper.sendMessage(replica1Port, new Message(MessageType.WRITE_FROM_CLIENT, Integer.toString(tempValue), Integer.toString(tempValue - 1)));

                // Reading
                answer = ReplicaTestHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, Integer.toString(tempValue - 1)));
                assertEquals(Integer.toString(tempValue - 1), answer.getResource());
                assertEquals(Integer.toString(tempValue - 2), answer.getValue());
            }

            // Closing replica and tracker
            replica1.interrupt();
            tracker.interrupt();
        } catch (IOException | ClassNotFoundException e) {
            fail();
        }
    }

    // This test runs a lot of replicas with only 1 tracker and 1 client
    @Test
    public void replicaSpam() {
        try {
            // Starting the tracker
            trackerPort = ReplicaTestHelper.getPort();
            tracker = new Thread(() -> Tracker.main(new String[]{Integer.toString(trackerPort)}));
            tracker.start();

            // Starting the first replica
            replica1Port = ReplicaTestHelper.getPort();
            replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(replica1Port)}));
            replica1.start();
            SimulateDelay.fixed(100);

            // Starting N other replicas
            for (int i = 0; i < N; i++) {
                replica1 = new Thread(() -> Replica.main(new String[]{LOCALHOST, Integer.toString(trackerPort), LOCALHOST, Integer.toString(ReplicaTestHelper.getPort())}));
                replica1.start();
                SimulateDelay.fixed(100);
            }

            // Starting the client
            answer = ReplicaTestHelper.sendMessageAndReceive(trackerPort, new Message(MessageType.ADD_CLIENT));

            // Write of the client on replica1
            ReplicaTestHelper.sendMessage(replica1Port, new Message(MessageType.WRITE_FROM_CLIENT, "x", "1"));

            // Read of the client on replica1
            answer = ReplicaTestHelper.sendMessageAndReceive(replica1Port, new Message(MessageType.READ_FROM_CLIENT, "x"));
            assertEquals("x", answer.getResource());
            assertEquals("1", answer.getValue());

            // Closing replica and tracker
            replica1.interrupt();
            tracker.interrupt();
        } catch (IOException | ClassNotFoundException e) {
            fail();
        }
    }
}
