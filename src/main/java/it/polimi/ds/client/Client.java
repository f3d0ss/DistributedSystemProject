package it.polimi.ds.client;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private final Address serverAddress;
    private static boolean done = false;
    private static final Logger logger = Logger.getLogger("Client");

    public static void main(String[] args) {
        Client client = new Client(args[0], args[1]);
        while(!done)
            client.start();
        logger.log(Level.INFO,"This client is now closed.");
    }

    public Client(String serverIP, String serverPort) {
        this.serverAddress = new Address(serverIP, Integer.parseInt(serverPort));
    }

    public void start() {
        TCPClient serverSocket;
        Message inputMessage;
        try {
            serverSocket = TCPClient.connect(serverAddress);
            serverSocket.out().writeObject(new Message(MessageType.ADD_CLIENT));
            inputMessage = (Message) serverSocket.in().readObject();
            serverSocket.close();
            logger.log(Level.INFO, () -> "Connected to server: " + serverAddress.toString());
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Impossible to reach the server, exiting.");
            return;
        }

        try {
            Address replicaAddress = inputMessage.getAddress();
            TCPClient replicaSocket;
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                String inputString = reader.readLine();
                String[] splittedString = inputString.split(" ");
                switch (splittedString[0]) {
                    // Reading a value, inputString = read <resource>
                    case "read":
                        replicaSocket = TCPClient.connect(replicaAddress);
                        replicaSocket.out().writeObject(new Message(MessageType.READ_FROM_CLIENT, splittedString[1]));
                        inputMessage = (Message) serverSocket.in().readObject();
                        System.out.println("Resource " + inputMessage.getResource() + " has value " + inputMessage.getValue() + ".");
                        replicaSocket.close();
                        break;
                    // Writing a value, inputString = write <resource> <value>
                    case "write":
                        replicaSocket = TCPClient.connect(replicaAddress);
                        replicaSocket.out().writeObject(new Message(MessageType.WRITE_FROM_CLIENT, splittedString[1], splittedString[2]));
                        replicaSocket.close();
                        logger.log(Level.INFO,"Value correctly registered.");
                        break;
                    // Exiting the client, inputString = exit
                    case "exit":
                        serverSocket = TCPClient.connect(serverAddress);
                        serverSocket.out().writeObject(new Message(MessageType.REMOVE_CLIENT, replicaAddress));
                        serverSocket.close();
                        setDone();
                        return;
                    // Every other expression is ignored
                    default:
                        logger.log(Level.WARNING, "Could not parse correctly the message, please try again.");
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "The current replica is no longer available, try again in order to connect to another replica.");
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Could not parse correctly the message, please try again.");
        }
    }

    private static void setDone() {
        done = true;
    }
}
