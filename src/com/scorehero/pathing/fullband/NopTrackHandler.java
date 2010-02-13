package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

public class NopTrackHandler extends TrackHandler {
    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        String theLine = in.readLine();
        do {
            theLine = in.readLine();
        } while(!"TrkEnd".equals(theLine));
        return in.readLine();
    }
}

