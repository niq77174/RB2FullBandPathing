package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.Set;
import java.util.HashSet;

public abstract class TrackHandler {
    public final static Set< String > EXPERT_NOTES = buildExpertNotes();
    public final static String OVERDRIVE_NOTE = "n=116";
    public final static short BASE_SCORE = 25;
    public final static int TICKS_PER_FRETBOARD_BEAT = 480;
    public final static int SIXTEENTH_TICKS = TICKS_PER_FRETBOARD_BEAT / 4;;
    public final static int TICKS_PER_POINT = TICKS_PER_FRETBOARD_BEAT / 12;
    public final static double WHAMMY_PER_TICK = 0.00226875;

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
