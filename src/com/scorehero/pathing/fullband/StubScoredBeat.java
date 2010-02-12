package com.scorehero.pathing.fullband;
import java.util.Collection;

public class StubScoredBeat extends ScoredBeat {

    public StubScoredBeat() {
        super();
    }

    public void addScore(BandState bandState, int score) {
        // should never happen!
    }

    public int getScore(BandState bandState) {
        return 0;
    }

    public void flush(String title, int beatNumber) {
    }

    public void close() {
    }

}
