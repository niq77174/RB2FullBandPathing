package com.scorehero.pathing.fullband;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Collection;

public class ScoredBeat implements Serializable {
    private HashMap< Integer, ScoredBandState > bandStates;

    private ScoredBeat() {
        this.bandStates = new HashMap< Integer, BandState >();
    }

    public void addNewScore(BandState bandState, int score) {
    }

    public int findBestScore(Collection< BandState > nextBandStates) {
    }

    public void findBandStatesByScore(int score) {
    }

    public void writeToFile(String fileName) {
    }

    public static ScoredBeat readFromFile {
    }
}
