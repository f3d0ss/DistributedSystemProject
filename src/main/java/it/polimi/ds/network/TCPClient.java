package it.polimi.ds.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.NotYetConnectedException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPClient {

    private Socket connectedSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static final Logger logger = Logger.getLogger("TCPClient");

    public TCPClient(Socket connectedSocket) throws IOException {
        if (!connectedSocket.isConnected())
            throw new NotYetConnectedException();

        this.connectedSocket = connectedSocket;
        out = new ObjectOutputStream(connectedSocket.getOutputStream());
        in = new ObjectInputStream(connectedSocket.getInputStream());
    }

    public static TCPClient connect(String hostname, int port) throws IOException {
        return new TCPClient(new Socket(hostname, port));
    }

    public static TCPClient connect(Address address) throws IOException {
        return new TCPClient(new Socket(address.getIp(), address.getPort()));
    }

    public ObjectInputStream in() {
        return in;
    }

    public ObjectOutputStream out() {
        return out;
    }

    public void close() {
        try {
            connectedSocket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "IOException, Class TCPClient", e);
        }
    }
}