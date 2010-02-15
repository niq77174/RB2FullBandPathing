package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

public class TempoTrackHandler extends TrackHandler {
    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        String theLine = null;
        int lastTempo = 0; // tempo is in microseconds/beat
        int lastTempoStart = 0;

        while(!"TrkEnd".equals(theLine = in.readLine())) {
            StringTokenizer tok = new StringTokenizer(theLine, " ");
            int ticks = Integer.valueOf(tok.nextToken()).intValue();
            String command = tok.nextToken();

            if ("Tempo".equals(command)) {
                if (ticks > 0 ) {
                    int lastTempoEnd = ticks;
                    BeatInfo currentBeat = result.getNearestBeat(lastTempoStart);
                    do {
                        int beatTicks = currentBeat.endTicks() - currentBeat.startTicks();
                        int duration = (lastTempo * beatTicks) / TrackHandler.TICKS_PER_FRETBOARD_BEAT;
                        currentBeat.addDuration(duration);
                        currentBeat = result.getBeat(currentBeat.beatNumber()+1);
                    } while (lastTempoEnd > currentBeat.startTicks());
                }

                lastTempo = Integer.valueOf(tok.nextToken()).intValue();
                lastTempoStart = ticks;
                continue;
            }

            if ("Meta".equals(command)) {
                if (!"TrkEnd".equals(tok.nextToken())) {
                    continue;
                }

                BeatInfo firstBeat = result.getNearestBeat(lastTempoStart);
                for (int i = firstBeat.beatNumber(); i < result.beats().size(); ++i) {
                    BeatInfo currentBeat = result.getBeat(i);
                    int beatTicks = currentBeat.endTicks() - currentBeat.startTicks();
                    int duration = (lastTempo * beatTicks) / TrackHandler.TICKS_PER_FRETBOARD_BEAT;
                    currentBeat.addDuration(duration);
                }

                continue;
            }
        } 
        return in.readLine();
    }
}

