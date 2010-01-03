package com.scorehero.pathing.fullband;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Collection;

public class ScoredBandState implements Serializable {
    private BandState bandState;
    int score;

    public ScoredBandState(BandState bandState, int score) {
    }

    public int score() {
        return this.score;
    }

    public BandState bandState() {
        return this.bandState;
    }

    // seralization TODO
}
