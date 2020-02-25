package it.polimi.ds.replica;

import it.polimi.ds.network.Address;
import it.polimi.ds.network.Message;
import it.polimi.ds.network.MessageType;
import it.polimi.ds.network.TCPClient;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Replica {
    public static final int REPLICA_PORT = 2222; // will be taken from environment
    private Address replicaAddress;
    private Address trackerAddress;
    private ArrayList<Address> otherReplicaAddresses;
    private StateHandler state;

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
            e.printStackTrace();
        }
//        Try to get the state from one of the replicas
        for (Address otherReplica: otherReplicaAddresses){
            try {
                getState(TCPClient.connect(otherReplica));
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void joinNetwork(TCPClient client) throws IOException {
        client.out().writeObject(new Message(MessageType.ADD_REPLICA, replicaAddress));
        try {
            otherReplicaAddresses = ((Message) client.in().readObject()).getAddressSet();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void getState(TCPClient client) throws IOException {
        client.out().writeObject(new Message(MessageType.GET_STATE));
        try {
            state = new StateHandler(((Message) client.in().readObject()).getState());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}