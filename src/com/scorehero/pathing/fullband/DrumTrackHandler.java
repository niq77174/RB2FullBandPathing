package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

public class DrumTrackHandler extends TrackHandler {
    private Instrument instrument;
    private int maxMultiplier;

    public DrumTrackHandler(Instrument instrument, int maxMultiplier) {
        this.instrument = instrument;
        this.maxMultiplier = maxMultiplier;
    }

    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        int streak = 0;
        int lastNoteTicks = 0;
        int overdriveStart = 0;
        boolean inOverdrive = false;

        String theLine = null;
        while(!"TrkEnd".equals(theLine = in.readLine())) {
            StringTokenizer tok = new StringTokenizer(theLine, " ");
            int ticks = Integer.valueOf(tok.nextToken()).intValue();
            String onOff = tok.nextToken();

            if ("On".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();
                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    System.out.println("OD start found: " + ticks);
                    overdriveStart = ticks;
                    inOverdrive = true;
                    continue;
                }

                if (!TrackHandler.EXPERT_NOTES.contains(note)) {
                    continue;
                }

                ++streak;

                int multiplier = Math.min(1 + streak / 10, 4);
                
                // now the score for the base note
                BeatInfo currentBeat = result.getNearestBeat(ticks);
                int chordScore = multiplier * TrackHandler.BASE_SCORE;
                currentBeat.addScore(this.instrument, chordScore);
                lastNoteTicks = ticks;
            } else if ("Off".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();

                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    BeatInfo currentBeat = result.getNearestBeat(ticks);
                    currentBeat.setOverdrivePhraseEnd(instrument, true);
                    BeatInfo lastChordBeat = result.getNearestBeat(lastNoteTicks);
                    lastChordBeat.setLastOverdriveNote(instrument, true);
                    inOverdrive = false;
                    continue;
                }
            }
        }

        return in.readLine();
    }
}
