package it.polimi.ds.network;

import java.io.Serializable;

public class UpdateWithTracker implements Serializable {
    private Update update;
    private boolean sameTrackerIndex;

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
}
