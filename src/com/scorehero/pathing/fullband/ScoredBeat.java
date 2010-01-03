package com.scorehero.pathing.fullband;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Collection;

public class ScoredBeat implements Serializable {
    private HashMap< Integer, ScoredBandState > scoredBandStateMap;

    private ScoredBeat() {
        this.scoredBandStateMap = new HashMap< Integer, ScoredBandState >();
    }

    public void addNewScore(BandState bandState, int score) {
        this.scoredBandStateMap.put(new Integer(bandState.serializedData()),
                                    new ScoredBandState(bandState, score));
    }

    public int findBestScore(Collection< BandState > nextBandStates) {
        int result = 0;

        for (BandState bandState : nextBandStates) {
            ScoredBandState scoredBandState = 
                scoredBandStateMap.get(bandState.serializedData());
            if (null == scoredBandState) {
                // ruh-roh! This should never happen.
                continue;
            }
            result = Math.max(result, scoredBandState.score());
        }

        return result;
    }

    public void findBandStatesByScore(final int score, 
                                      Collection< BandState > result) {
        for (ScoredBandState scoredBandState : this.scoredBandStateMap.values()) {
            if (scoredBandState.score() == score) {
                result.add(scoredBandState.bandState());
            }
        }
    }

    public void writeToFile(String fileName) {
    }

    public static ScoredBeat readFromFile(String fileName) {
        return null;
    }
}
