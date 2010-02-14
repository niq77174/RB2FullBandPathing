package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

public class GuitarTrackHandler extends TrackHandler {
    private Instrument instrument;
    private int maxMultiplier;

    public GuitarTrackHandler(Instrument instrument, int maxMultiplier) {
        this.instrument = instrument;
        this.maxMultiplier = maxMultiplier;
    }

    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        int chordCount = 0;
        int streak = 0;
        int lastChordTicks = 0;
        int overdriveStart = 0;
        boolean inOverdrive = false;
        boolean isSustain = false;
        boolean isSustainThroughPhraseEnd = false;
        double whammy = 0.0;
        int lastSavedChord = 0;

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

                if (!TrackHandler.EXPERT_NOTES.contains(note)) {
                    continue;
                }

                // reset the chord
                if (ticks != lastChordTicks) {
                    chordCount = 0;
                    lastChordTicks = ticks;
                }

                isSustain = true;
                if (inOverdrive) {
                    isSustainThroughPhraseEnd = false;
                }
                ++chordCount;

            } else if ("Off".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();

                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    BeatInfo currentBeat = result.getNearestBeat(ticks);
                    currentBeat.setOverdrivePhraseEnd(instrument, true);
                    int lastODChordTicks = lastSavedChord;

                    if (isSustain) {
                        isSustainThroughPhraseEnd = true;
                        lastODChordTicks = lastChordTicks;
                    }

                    BeatInfo lastChordBeat = result.getNearestBeat(lastODChordTicks);
                    lastChordBeat.setLastOverdriveNote(instrument, true);
                    inOverdrive = false;
                    continue;
                }

                if (!TrackHandler.EXPERT_NOTES.contains(note)) {
                    continue;
                }

                if (ticks == lastChordTicks) {
                    // System.out.println("something strange has happened");
                    continue;
                }

                if (0 == lastChordTicks) {
                    System.out.println("off before on?!?");
                    lastChordTicks = ticks;
                    ++chordCount;
                    continue;
                }

                ++streak;
                lastSavedChord = lastChordTicks;

                // time to score this chord. First, the multiplier
                int multiplier = Math.min(1 + streak / 10, this.maxMultiplier);
                
                // now the score for the base note
                BeatInfo currentBeat = result.getNearestBeat(lastChordTicks);
                int chordScore = chordCount * multiplier * TrackHandler.BASE_SCORE;
                currentBeat.addScore(this.instrument, chordScore);

                // now any sustain
                if (ticks - lastChordTicks > TrackHandler.SIXTEENTH_TICKS) {
                    do {

                        // sustain score
                        int thisBeatsTicks = 
                            Math.min(ticks, currentBeat.endTicks()) -
                            Math.max(lastChordTicks, currentBeat.startTicks());
                        int sustainScore = chordCount * multiplier * thisBeatsTicks / TrackHandler.TICKS_PER_POINT;
                        currentBeat.addScore(this.instrument, sustainScore);

                        // whammy
                        if (inOverdrive || isSustainThroughPhraseEnd) {
                            whammy += thisBeatsTicks * TrackHandler.WHAMMY_PER_TICK;
                            byte thisBeatsWhammy = (byte) Math.floor(whammy);
                            currentBeat.setWhammy(this.instrument, thisBeatsWhammy);
                            whammy -= thisBeatsWhammy;
                        }

                        currentBeat = result.getBeat(currentBeat.beatNumber()+1);
                    } while (ticks > currentBeat.startTicks());

                    // done processing this chord's sustain; time to clear
                    // sustain state
                    isSustainThroughPhraseEnd = false;
                }

                // advance to the next chord
                isSustain = false;
                chordCount = 0;
                lastChordTicks = ticks;
            }
        }

        return in.readLine();
    }
}
