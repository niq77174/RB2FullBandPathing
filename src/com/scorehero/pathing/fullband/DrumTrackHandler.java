package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.HashSet;

public class DrumTrackHandler extends TrackHandler {
        private Instrument instrument;
        private int maxMultiplier;

        public final static int MIN_FILL_DURATION = 1200000;
        private final static Set< String > FILL_BRE_NOTES = buildFillBRENotes();
        private final static Set< String > buildFillBRENotes() {
            Set< String > result = new HashSet< String >();
            result.add("n=120");
            result.add("n=121");
            result.add("n=122");
            result.add("n=123");
            result.add("n=124");
            return result;
        }

    public DrumTrackHandler(Instrument instrument, int maxMultiplier) {
        this.instrument = instrument;
        this.maxMultiplier = maxMultiplier;
    }

    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        int streak = 0;
        int lastNoteTicks = 0;
        int overdriveStart = 0;
        boolean inOverdrive = false;
        int fillStart = 0;
        int lastOverdriveNote = 0;

        String theLine = null;
        while(!"TrkEnd".equals(theLine = in.readLine())) {
            StringTokenizer tok = new StringTokenizer(theLine, " ");
            int ticks = Integer.valueOf(tok.nextToken()).intValue();
            String onOff = tok.nextToken();

            if ("On".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();
                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    overdriveStart = ticks;
                    inOverdrive = true;
                    continue;
                }

                if (FILL_BRE_NOTES.contains(note)) {
                    fillStart = ticks;
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

                if (inOverdrive) {
                    lastOverdriveNote = ticks;
                }
            } else if ("Off".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();

                if (FILL_BRE_NOTES.contains(note)) {
                    int fillEnd = ticks;
                    BeatInfo currentBeat = result.getNearestBeat(fillStart);
                    int odToFillDuration = computeDuration(lastOverdriveNote, fillStart, result);
                    do {
                        currentBeat.setIsDrumFill(true);
                        currentBeat.setODToFillDuration(odToFillDuration);
                        currentBeat = result.getBeat(currentBeat.beatNumber()+1);
                    } while (fillEnd > currentBeat.startTicks());
                    continue;
                }

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

        // go ahead and set the activation
        for (int i = 1; i < result.beats().size(); ++i) {
            BeatInfo previousBeat = result.getBeat(i-1);
            BeatInfo currentBeat = result.getBeat(i);
            if (previousBeat.isDrumFill() && !currentBeat.isDrumFill()) {
                currentBeat.setInstrumentCanActivate(Instrument.DRUMS, true);
            }
        }

        return in.readLine();
    }

    private static int computeDuration(int intervalStart, int intervalEnd, SongInfo result) {
        BeatInfo currentBeat = result.getNearestBeat(intervalStart);
        int duration = 0;
        do {
            int thisBeatTicks = currentBeat.endTicks() - currentBeat.startTicks();
            int ticksInInterval = 
                Math.min(intervalEnd, currentBeat.endTicks()) -
                Math.max(intervalStart, currentBeat.startTicks());
            duration += (currentBeat.duration()*ticksInInterval) / thisBeatTicks;
            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
        } while (intervalEnd > currentBeat.startTicks());

        return duration;
    }
}
