package it.polimi.ds.client;

import it.polimi.ds.network.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private static final Logger logger = Logger.getLogger("Client");
    private static boolean done = false;
    private final Address serverAddress;
    private int minDelay = 0;
    private int maxDelay = 0;

    private Client(String serverIP, String serverPort, int minDelay, int maxDelay) {
        this(serverIP, serverPort);
        if (maxDelay > 0)
            this.maxDelay = maxDelay;
        if (minDelay > 0 && minDelay <= this.maxDelay)
            this.minDelay = minDelay;
    }

    private Client(String serverIP, String serverPort) {
        this.serverAddress = new Address(serverIP, Integer.parseInt(serverPort));
    }

    public static void main(String[] args) {
        Client client;
        if (args.length >= 4)
            client = new Client(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        else if (args.length >= 2)
            client = new Client(args[0], args[1]);
        else {
            logger.log(Level.SEVERE, "Too few arguments, client was not launched.");
            logger.log(Level.SEVERE, () -> "Please launch the client with" +
                    "<serverIP> <serverPort> [<minDelay> <maxDelay>] as parameters.");
            return;
        }
        welcomeMessage();
        while (!done)
            client.start();
        logger.log(Level.INFO, "This client is now closed.");
    }

    private static void setDone() {
        done = true;
    }

    private static void welcomeMessage() {
        logger.log(Level.INFO, "Usage:" +
                "\nread <resource-name>             Displays value of the resource" +
                "\nwrite <resource-name> <value>    Sets new value for the resource" +
                "\nexit                             Terminates program");
    }

    private void start() {
        TCPClient serverSocket;
        Message inputMessage;
        try {
            SimulateDelay.uniform(minDelay, maxDelay);
            serverSocket = TCPClient.connect(serverAddress);
            serverSocket.out().writeObject(new Message(MessageType.ADD_CLIENT));
            inputMessage = (Message) serverSocket.in().readObject();
            serverSocket.close();
            logger.log(Level.INFO, () -> "Connected to tracker server: " + serverAddress.toString());
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Impossible to reach the tracker server: Enter exit to quit, Enter anything else to retry");
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            try {
                if (r.readLine().equals("exit")) setDone();
            } catch (IOException ex) {
                logger.log(Level.WARNING, e.getMessage());
            }
            return;
        }

        try {
            Address replicaAddress = inputMessage.getAddress();
            if (replicaAddress == null) { //avoid nullpointer when no replicas are available
                logger.log(Level.INFO, "There are no replicas available, press Enter to retry");
                new BufferedReader(new InputStreamReader(System.in)).readLine();
                return;
            }
            TCPClient replicaSocket;
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String inputString = reader.readLine();
                String[] splittedString = inputString.split(" ");
                switch (splittedString[0]) {
                    // Reading a value, inputString = read <resource>
                    case "read":
                        if (splittedString.length != 2) {
                            logger.log(Level.INFO, "Usage: read <resource-name>");
                            break;
                        }
                        SimulateDelay.uniform(minDelay, maxDelay);
                        replicaSocket = TCPClient.connect(replicaAddress);
                        replicaSocket.out().writeObject(new Message(MessageType.READ_FROM_CLIENT, splittedString[1]));
                        inputMessage = (Message) replicaSocket.in().readObject();
                        if (inputMessage.getResource() == null || inputMessage.getValue() == null)
                            throw new IOException();
                        System.out.println("Resource " + inputMessage.getResource() + " has value " + inputMessage.getValue() + ".");
                        replicaSocket.close();
                        break;
                    // Writing a value, inputString = write <resource> <value>
                    case "write":
                        if (splittedString.length != 3) {
                            logger.log(Level.INFO, "Usage: write <resource-name> <value>");
                            break;
                        }
                        SimulateDelay.uniform(minDelay, maxDelay);
                        replicaSocket = TCPClient.connect(replicaAddress);
                        replicaSocket.out().writeObject(new Message(MessageType.WRITE_FROM_CLIENT, splittedString[1], splittedString[2]));
                        inputMessage = (Message) replicaSocket.in().readObject();
                        replicaSocket.close();
                        if (inputMessage.getType() != MessageType.ACK)
                            throw new IOException();
                        logger.log(Level.INFO, "Value correctly registered.");
                        break;
                    // Exiting the client, inputString = exit
                    case "exit":
                        SimulateDelay.uniform(minDelay, maxDelay);
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
}
