package com.scorehero.pathing.fullband;
import java.util.Collection;

public abstract class ScoredBeat {

    protected ScoredBeat() {
    }

    public abstract void addScore(BandState bandState, int score);

    public abstract int getScore(BandState bandState);
    public int getScore(BandState previousState, BandState bandState) {
        int result = this.getScore(bandState);
        if (-1 == result) {
            throw new RuntimeException("Couldn't find score: prev/current:\n " + previousState + "\n" + bandState);
            // ruh-roh! This should never happen.
        }

        return result;
        
    }

    public abstract void flush(String title, int beatNumber) throws Exception ;
    public abstract void close();

    public static ScoredBeat fromFile(String songTitle, int beatNumber) {
        return new BDBScoredBeat(songTitle, beatNumber);
    }
}
