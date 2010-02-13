package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

public class BeatTrackHandler extends TrackHandler {
    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        String theLine = in.readLine();
        int start = 0;
        short trackBeatCount = 0;
        short measureNumber = 0;
        short lastDownBeat = 0;
        short dpBeatCount = 0;
        do {
            // need to update
            //System.out.println("Beat track handler: " + theLine);
            StringTokenizer tok = new StringTokenizer(theLine, " ");
            int position = Integer.valueOf(tok.nextToken()).intValue();

            String onOff = tok.nextToken();
            //System.out.println(position + "/" + onOff);
            if ("On".equals(onOff)) {
                tok.nextToken();
                if(position > 0) {
                    for (int i = 0; i < SongInfo.SUBBEATS_PER_BEAT; ++i) {
                        int beatStart = start + i /(position - start) / SongInfo.SUBBEATS_PER_BEAT;
                        int beatEnd = beatStart + (position - start) / SongInfo.SUBBEATS_PER_BEAT;
                        result.addBeat(beatStart, beatEnd);
                        ++dpBeatCount;
                    }
                }

                if ("n=12".equals(tok.nextToken())) {
                    short measureBeats = (short) (dpBeatCount - lastDownBeat);
                    for (int i = 0; i < measureBeats; ++i) {
                        result.getBeat(lastDownBeat+i).setMeasure(measureNumber, (double) i / (double) measureBeats);
                    }
                    lastDownBeat = dpBeatCount;
                    ++measureNumber;
                }

                start = position;
            }

            if ("TrkEnd".equals(tok.nextToken())) {
                for (int i = 0; i < SongInfo.SUBBEATS_PER_BEAT; ++i) {
                    int beatStart = start + i /(position - start) / SongInfo.SUBBEATS_PER_BEAT;
                    int beatEnd = beatStart + (position - start) / SongInfo.SUBBEATS_PER_BEAT;
                    result.addBeat(beatStart, beatEnd);
                    ++dpBeatCount;
                }

                short measureBeats = (short) (dpBeatCount - lastDownBeat);
                for (int i = 0; i < measureBeats; ++i) {
                    result.getBeat(lastDownBeat+i).setMeasure(measureNumber, (double) i / (double) measureBeats);
                }

                start = position;
                lastDownBeat = dpBeatCount;
            }

            ++trackBeatCount;
            theLine = in.readLine();
        } while (!"TrkEnd".equals(theLine));

        return in.readLine();
    }
}
