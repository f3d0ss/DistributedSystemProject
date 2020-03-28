package it.polimi.ds.replica;

import it.polimi.ds.network.Message;
import it.polimi.ds.network.TCPClient;

import java.io.IOException;

public class ReplicaTestHelper {
    private static final String LOCALHOST = "127.0.0.1";
    private static int port = 2222; // Incremented every time in order to avoid trackers/replicas with the same port

    public static void sendMessage(String address, int port, Message message) throws IOException {
        TCPClient client = TCPClient.connect(address, port);
        client.out().writeObject(message);
        client.close();
    }

    public static void sendMessage(int port, Message message) throws IOException {
        sendMessage(LOCALHOST, port, message);
    }

    public static Message sendMessageAndReceive(String address, int port, Message message) throws IOException, ClassNotFoundException {
        TCPClient client = TCPClient.connect(address, port);
        client.out().writeObject(message);
        Message answer = (Message) client.in().readObject();
        client.close();
        return answer;
    }

    public static Message sendMessageAndReceive(int port, Message message) throws IOException, ClassNotFoundException {
        return sendMessageAndReceive(LOCALHOST, port, message);
    }

    public static int getPort() {
        port++;
        return port;
    }
}
