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
        private final static int VOCAL_ACTIVATION_DELAY = 250000;
        private final static int VOCAL_UNREACHABLE = 120000;

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
        int phraseStartTicks = 0;
        int phraseEndTicks = 0;
        boolean inOverdrive = false;
        boolean hitPhraseStart = false;
        boolean singing = false;
        boolean sangThroughPhraseStart = false;
        boolean taps = false;

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
                    phraseStartTicks = ticks;
                    hitPhraseStart = true;
                    continue;
                }

                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    overdriveStart = ticks;
                    inOverdrive = true;
                    continue;
                }

                if (!VOCAL_NOTES.contains(note)) {
                    continue;
                }

                if (VOCAL_PERC_NOTE.equals(note)) {
                    taps = true;
                } else {
                    startTicks.add(new Integer (ticks));
                    taps = false;
                }

                if (hitPhraseStart) {
                    if (taps) {
                        computeActivationPoints(phraseEndTicks, phraseStartTicks, result);
                    } else {
                        computeActivationPoints(lastNoteEnd, ticks, result);
                    }
                    hitPhraseStart = false;
                }

                lastNoteStart = ticks;
                singing = true;
            } else if ("Off".equals(onOff)) {
                tok.nextToken();
                String note = tok.nextToken();

                if (VOCAL_P1_NOTE.equals(note) || VOCAL_P2_NOTE.equals(note)) {
                    phraseEndTicks = ticks;
                    // mark the phrase ending
                    result.getNearestBeat(lastNoteEnd).setVocalPhraseEnd(true);

                    

                    //compute score
                    ++streak;
                    int phraseScore = Math.min(streak, 4) * VOCAL_PHRASE_BASE_SCORE;
                    int pointsLeft = phraseScore;
                    double invPhraseTicks = 1.0 / (double) thisPhraseTicks;


                    // this is a hack to make sure we defer scoring if we're singing through the end of a phrase marker
                    if(!singing) {
                        assert(startTicks.size() == endTicks.size());
                        computePhrasePoints(startTicks, endTicks, thisPhraseTicks, phraseScore, result);
                        thisPhraseTicks = 0;
                        startTicks.clear();
                        endTicks.clear();
                    } else {
                        sangThroughPhraseStart = true;
                    }

                    continue;
                }

                if (TrackHandler.OVERDRIVE_NOTE.equals(note)) {
                    BeatInfo currentBeat = result.getNearestBeat(ticks-1);
                    // for vocals the phrase and and the OD note are effectively equal
                    currentBeat.setOverdrivePhraseEnd(instrument, true);
                    //BeatInfo lastChordBeat = result.getNearestBeat(lastNoteEnd);
                    currentBeat.setLastOverdriveNote(instrument, true);
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

                // need to be apply the score if we sang through the phrase start
                if (sangThroughPhraseStart) {
                    int phraseScore = Math.max(Math.min(streak, 4),1) * VOCAL_PHRASE_BASE_SCORE;
                    computePhrasePoints(startTicks, endTicks, thisPhraseTicks, phraseScore, result);
                    thisPhraseTicks = 0;
                    startTicks.clear();
                    endTicks.clear();
                    sangThroughPhraseStart = false;
                }

                singing = false;
                lastNoteEnd = ticks;
            }
        }

        computeActivationPoints(lastNoteEnd, 
                                result.getBeat(result.beats().size()-1).endTicks(),
                                result);

        return in.readLine();
    }

    private void computePhrasePoints(ArrayList< Integer > startTicks, 
                                     ArrayList< Integer > endTicks, 
                                     int phraseTicks, int phraseScore,
                                     SongInfo result) {
        assert(startTicks.size() == endTicks.size());
        double invPhraseTicks = 1.0 / (double) phraseTicks;
        int pointsLeft = phraseScore;

        for (int i = 0; i < startTicks.size(); ++i) {
            assert(startTicks.size() == endTicks.size());
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
    }

    private static void computeActivationPoints(int intervalStart, 
                                                int intervalEnd, 
                                                SongInfo result) { 


        if (SongInfo.DEBUG_OUTPUT) {
            System.out.println("Computing activation points between " + intervalStart + " and " +intervalEnd);
        }
        BeatInfo startBeat = result.getNearestBeat(intervalStart);
        BeatInfo currentBeat = startBeat;
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

            if (currentBeat.beatNumber() == result.beats().size()-1) {
                assert(intervalEnd >= currentBeat.endTicks());
                break;
            }

            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
        } while (intervalEnd > currentBeat.startTicks());

        if (duration < VOCAL_ACTIVATION_PHRASE_MINIMUM) {
            return;
        }

        // this is tricky. What we actually care about is not "when is the singer capable of wooing into activation",
        // but "when does the state transition occur". There's an estimated 250 msec delay between the two
        int delay = durationInFirstBeat;
        if (SongInfo.DEBUG_OUTPUT) {
            System.out.println("singing duration in first beat: " + durationInFirstBeat);
        }
        currentBeat = startBeat;
        while (delay < VOCAL_ACTIVATION_DELAY) {
            if (SongInfo.DEBUG_OUTPUT) {
                System.out.println("skipping beat: " + currentBeat.measureNumber());
            }
            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
            delay += currentBeat.duration();
        }


        // the length of available activations is about 120 msec shorter than the visible length of the activation
        // window
        final int vocalWindow = duration - VOCAL_UNREACHABLE;

        // I should really do something about phrases ending halfway through a
        // beat. Starting is okay but ending is not
        currentBeat.setInstrumentCanActivate(Instrument.VOCALS, true);
        int durationUsed = delay - VOCAL_ACTIVATION_DELAY;
        if (SongInfo.DEBUG_OUTPUT) {
            System.out.println("Marking beat " + currentBeat.measureNumber());
            System.out.println("delay duration in first beat: " + durationUsed);
        }
        //int durationUsed = 0;
        while (durationUsed < vocalWindow && (currentBeat.beatNumber()+1) < result.beats().size()) {
            currentBeat = result.getBeat(currentBeat.beatNumber()+1);
            if (SongInfo.DEBUG_OUTPUT) {
                System.out.println("Marking beat " + currentBeat.measureNumber());
            }
            currentBeat.setInstrumentCanActivate(Instrument.VOCALS, true);
            durationUsed += currentBeat.duration();
        }
    }

    private static void computePhraseScore(ArrayList< Integer > noteStarts, ArrayList< Integer > noteEnds, 
                                           int score, SongInfo result) {
    }
}
