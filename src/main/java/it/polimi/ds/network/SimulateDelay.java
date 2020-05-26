package it.polimi.ds.network;

import java.util.Random;

/**
 * Used to simulate a delay of the network in order to perform testing under slow connections.
 */
public class SimulateDelay {
    private static final int FIXED_DELAY = 1000;
    private static final int MIN_DELAY = 1000;
    private static final int MAX_DELAY = 5 * 1000;
    private static final Random rand = new Random();

    private SimulateDelay() {
    }

    public static void fixed(int delay) {
        if (delay > 0)
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }

    public static void fixed() {
        SimulateDelay.fixed(FIXED_DELAY);
    }

    public static void uniform(int from, int to) {
        if (to > from)
            try {
                Thread.sleep((long) rand.nextInt(to - from) + to);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        else
            SimulateDelay.fixed(from);
    }

    public static void uniform(int to) {
        SimulateDelay.uniform(MIN_DELAY, to);
    }

    public static void uniform() {
        SimulateDelay.uniform(MIN_DELAY, MAX_DELAY);
    }
}
