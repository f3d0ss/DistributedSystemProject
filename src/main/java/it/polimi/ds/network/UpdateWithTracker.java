package it.polimi.ds.network;

import java.io.Serializable;
import java.util.Objects;

/**
 * This is a wrapper for the update
 * It's used to add the sameTrackerIndex attribute before add the update into the queue of update
 */
public class UpdateWithTracker implements Serializable, Comparable<Object> {
    private final Update update;
    private final boolean sameTrackerIndex;

    public UpdateWithTracker(Update update, boolean sameTrackerIndex) {
        this.update = update;
        this.sameTrackerIndex = sameTrackerIndex;
    }

    public Update getUpdate() {
        return update;
    }

    public boolean isSameTrackerIndex() {
        return sameTrackerIndex;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof UpdateWithTracker) {
            UpdateWithTracker up = (UpdateWithTracker) o;
            return update.compareTo(up.update);
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UpdateWithTracker)
            return ((UpdateWithTracker) obj).getUpdate().equals(update) && ((UpdateWithTracker) obj).isSameTrackerIndex() == sameTrackerIndex;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(update, sameTrackerIndex);
    }
}
