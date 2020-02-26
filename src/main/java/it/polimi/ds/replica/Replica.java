package it.polimi.ds.replica;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Replica {
    public static final int REPLICA_PORT = 2222; // will be taken from environment
    private Address replicaAddress;
    private Address trackerAddress;
    private List<Address> otherReplicaAddresses;
    private StateHandler state;
    private static final Logger logger = Logger.getLogger("Replica");

    public static void main(String[] args) {
        Replica tracker = new Replica();
        tracker.start(args[0], args[1], args[2], args[3]);
    }

    public void start(String trackerIp, String trackerPort, String replicaIp, String replicaPort) {
        this.trackerAddress = new Address(trackerIp, Integer.valueOf(trackerPort));
        this.replicaAddress = new Address(replicaIp, Integer.valueOf(replicaPort));
        try {
            joinNetwork(TCPClient.connect(trackerAddress));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Impossible to contact the server, exiting.");
            return;
        }
//        Try to get the state from one of the replicas
        for (Address otherReplica: otherReplicaAddresses){
            try {
                getState(TCPClient.connect(otherReplica));
                break;
            } catch (IOException e) {
                logger.log(Level.WARNING, () -> "Impossible to get a valid state from " + otherReplicaAddresses + ", trying an other one.");
            }
        }
    }


    private void joinNetwork(TCPClient client) throws IOException {
        client.out().writeObject(new Message(MessageType.ADD_REPLICA, replicaAddress));
        try {
            otherReplicaAddresses = ((Message) client.in().readObject()).getAddressSet();
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Could not read the message.");
        }
    }

    private void getState(TCPClient client) throws IOException {
        client.out().writeObject(new Message(MessageType.GET_STATE));
        try {
            state = new StateHandler(((Message) client.in().readObject()).getState());
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Could not read the message.");
        }
    }
}
