package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.Set;
import java.util.HashSet;

public abstract class TrackHandler {
    public final static Set< String > EXPERT_NOTES = buildExpertNotes();
    public final static String OVERDRIVE_NOTE = "n=116";
    public final static short BASE_SCORE = 25;
    public final static int SIXTEENTH_TICKS = 120;
    public final static int TICKS_PER_POINT = 40;
    public final static double WHAMMY_PER_TICK = 1.089 / ((double) (480 / SongInfo.SUBBEATS_PER_BEAT));

    private static Set< String > buildExpertNotes() {
        Set< String > result = new HashSet< String >();
        result.add("n=96");
        result.add("n=97");
        result.add("n=98");
        result.add("n=99");
        result.add("n=100");

        return result;
    }

    public abstract String handleTrack(LineNumberReader in, SongInfo result) throws Exception;
}
