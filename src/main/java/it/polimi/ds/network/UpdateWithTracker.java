package it.polimi.ds.network;

import java.io.Serializable;

public class UpdateWithTracker implements Serializable, Comparable {
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
}
