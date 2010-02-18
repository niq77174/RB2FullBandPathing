package com.scorehero.pathing.fullband;
import java.io.LineNumberReader;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;

public class VocalTrackHandler extends TrackHandler {
        private Instrument instrument;
        private int maxMultiplier;

        private final static Set< String > VOCAL_NOTES = buildVocalHashSet();
        private final static String VOCAL_P1_NOTE = "n=105";
        private final static String VOCAL_P2_NOTE = "n=106";
        private final static String VOCAL_PERC_NOTE = "n=96";
        private final static int VOCAL_PHRASE_BASE_SCORE = 1000;
        private final static int VOCAL_ACTIVATION_PHRASE_MINIMUM = 600000;
        private final static int VOCAL_UNREACHABLE = 180000;

    private static Set< String > buildVocalHashSet() {
        Set< String > result = new HashSet< String >();
        for (int i = 0; i < 49; ++i) {
            result.add("n=" + (36+i));
        }
        result.add(VOCAL_PERC_NOTE);

        return result;
    }

    public VocalTrackHandler(Instrument instrument, int maxMultiplier) {
        this.instrument = instrument;
        this.maxMultiplier = maxMultiplier;
    }

    public String handleTrack(LineNumberReader in, SongInfo result) throws Exception {
        int streak = 0;
        int lastNoteStart = 0;
        int lastNoteEnd = 0;
        int thisPhraseTicks = 0;
        int overdriveStart = 0;
        boolean inOverdrive = false;
        boolean hitPhraseStart = false;


        String theLine = null;
        ArrayList< Integer > startTicks = new ArrayList< Integer >();
        ArrayList< Integer > endTicks = new ArrayList< Integer >();

        while(!"TrkEnd".equals(theLine = in.readLine())) {
            StringTokenizer tok = new StringTokenizer(theLine, " ");
            int ticks = Integer.valueOf(tok.nextToken()).intValue();
            String onOff = tok.nextToken();

            if ("On".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();

                if (VOCAL_P1_NOTE.equals(note) || VOCAL_P2_NOTE.equals(note)) {
                    // compute activatable regions
                    hitPhraseStart = true;
                    continue;
                    // add vocal activation points
                }

                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    overdriveStart = ticks;
                    inOverdrive = true;
                    continue;
                }

                if (!VOCAL_NOTES.contains(note)) {
                    continue;
                }

                if (!VOCAL_PERC_NOTE.equals(note)) {
                    startTicks.add(new Integer (ticks));
                }

                if (hitPhraseStart) {
                    computeActivationPoints(lastNoteEnd, ticks, result);
                    hitPhraseStart = false;;
                }

                lastNoteStart = ticks;
            } else if ("Off".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();

                if (VOCAL_P1_NOTE.equals(note) || VOCAL_P2_NOTE.equals(note)) {
                    // mark the phrase ending
                    result.getNearestBeat(lastNoteEnd).setVocalPhraseEnd(true);

                    //compute score
                    ++streak;
                    int phraseScore = Math.min(streak, 4) * VOCAL_PHRASE_BASE_SCORE;
                    int pointsLeft = phraseScore;
                    double invPhraseTicks = 1.0 / (double) thisPhraseTicks;


                    if (SongInfo.DEBUG_OUTPUT) {
                        System.out.println("phrase end ticks " + lastNoteEnd);
                    }
                    for (int i = 0; i < startTicks.size(); ++i) {
                        int thisNoteStartTicks = startTicks.get(i).intValue();
                        int thisNoteEndTicks = endTicks.get(i).intValue();

                        BeatInfo currentBeat = result.getNearestBeat(thisNoteStartTicks);
                        do {
                            // sustain score
                            int thisBeatTicks = 
                                Math.min(thisNoteEndTicks, currentBeat.endTicks()) -
                                Math.max(thisNoteStartTicks, currentBeat.startTicks());
                            int thisBeatScore = (int) (((double) (phraseScore * thisBeatTicks)) * invPhraseTicks);
                            currentBeat.addScore(this.instrument, thisBeatScore);
                            pointsLeft -= thisBeatScore;

                            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
                        } while (thisNoteEndTicks > currentBeat.startTicks());
                    }

                    thisPhraseTicks = 0;
                    startTicks.clear();
                    endTicks.clear();
                    continue;
                }

                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    BeatInfo currentBeat = result.getNearestBeat(ticks);
                    currentBeat.setOverdrivePhraseEnd(instrument, true);
                    BeatInfo lastChordBeat = result.getNearestBeat(lastNoteEnd);
                    lastChordBeat.setLastOverdriveNote(instrument, true);
                    inOverdrive = false;
                    continue;
                }

                if (!VOCAL_NOTES.contains(note)) {
                    continue;
                }

                thisPhraseTicks += (ticks - lastNoteStart);
                if (!VOCAL_PERC_NOTE.equals(note)) {
                    endTicks.add(new Integer (ticks));
                }
                lastNoteEnd = ticks;
            }
        }

        return in.readLine();
    }

    // this isn't quite right
    private static void computeActivationPoints(int intervalStart, int intervalEnd, SongInfo result) {
        BeatInfo currentBeat = result.getNearestBeat(intervalStart);
        int duration = 0;
        int durationInFirstBeat = 0;
        do {
            int thisBeatTicks = currentBeat.endTicks() - currentBeat.startTicks();
            int ticksInInterval = 
                Math.min(intervalEnd, currentBeat.endTicks()) -
                Math.max(intervalStart, currentBeat.startTicks());
            if (0 == duration) {
                durationInFirstBeat = (currentBeat.duration()*ticksInInterval) / thisBeatTicks;
            }
            duration += (currentBeat.duration()*ticksInInterval) / thisBeatTicks;
            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
        } while (intervalEnd > currentBeat.startTicks());

        if (duration < VOCAL_ACTIVATION_PHRASE_MINIMUM) {
            return;
        }

        final int vocalWindow = duration - VOCAL_UNREACHABLE;

        // I should really do something about phrases ending halfway through a
        // beat. Starting is okay.
        currentBeat = result.getNearestBeat(intervalStart);
        currentBeat.setInstrumentCanActivate(Instrument.VOCALS, true);
        int ticksUsed = durationInFirstBeat;
        while (ticksUsed < vocalWindow) {
            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
            currentBeat.setInstrumentCanActivate(Instrument.VOCALS, true);
            ticksUsed += currentBeat.duration();
        }
        /*
        do {
            currentBeat.setInstrumentCanActivate(Instrument.VOCALS, true);
            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
        } while (intervalEnd > currentBeat.startTicks());
        */
    }

    private static void computePhraseScore(ArrayList< Integer > noteStarts, ArrayList< Integer > noteEnds, 
                                           int score, SongInfo result) {
    }
}
