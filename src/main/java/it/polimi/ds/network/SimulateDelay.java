package it.polimi.ds.network;

import java.util.Random;

public class SimulateDelay {
    private static final int FIXED_DELAY = 1000;
    private static final int MIN_DELAY = 1000;
    private static final int MAX_DELAY = 5 * 1000;
    private static final Random rand = new Random();

    private SimulateDelay() {
    }

    public static void fixed(int delay) {
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
        if(to > from)
            try {
                Thread.sleep((long)rand.nextInt(to - from) + to);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }

    public static void uniform(int to) {
        SimulateDelay.uniform(MIN_DELAY, to);
    }

    public static void uniform() {
        SimulateDelay.uniform(MIN_DELAY, MAX_DELAY);
    }
}
