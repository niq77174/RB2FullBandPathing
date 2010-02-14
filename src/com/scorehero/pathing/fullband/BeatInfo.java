package com.scorehero.pathing.fullband;

import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

public class BeatInfo implements Cloneable {
        short beatNumber;
        short measureNumber;
        double beatWithinMeasure; // between 0.0 and 1.0
        int beatStartTicks;
        int beatEndTicks;
        int score;
        int vocalScore;
        private int drumScore; // 0 if not covered by fill
        boolean hasOverdrivePhraseEnd[] = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        boolean hasLastOverdriveNote[] = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        boolean instrumentCanActivate[] = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        boolean canBeInOverdrive[] = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        boolean isReachableActiveMeter[][] = new boolean[Instrument.INSTRUMENT_COUNT.index()][SongInfo.OVERDRIVE_FULLBAR+1];
        byte maximumOverdriveBar[] = new byte[Instrument.INSTRUMENT_COUNT.index()];
        byte whammy[] = new byte[Instrument.INSTRUMENT_COUNT.index()];
        boolean isDrumFill = false;
        boolean hasLastBeatOfUnisonBonus = false;
        boolean isVocalPhraseEnd = false;
        int duration = 0; // microseconds
        private int odToFillDuration = 0;

    public BeatInfo() {
        this(0,0);
    }

    public BeatInfo(int startTicks, int endTicks) {
        this.beatStartTicks = startTicks;
        this.beatEndTicks = endTicks;
        this.instrumentCanActivate[Instrument.GUITAR.index()] =
            this.instrumentCanActivate[Instrument.BASS.index()] = true;
        this.canBeInOverdrive[Instrument.GUITAR.index()] = 
            this.canBeInOverdrive[Instrument.BASS.index()] = true;
        for (int i = 1; i < SongInfo.OVERDRIVE_FULLBAR + 1; ++i) {
            this.isReachableActiveMeter[Instrument.GUITAR.index()][i] =
            this.isReachableActiveMeter[Instrument.BASS.index()][i] = true;
        }
    }

    public void setMeasure(short measureNumber, double beatWithinMeasure) {
        //System.out.println("setting measure: " + (((double) measureNumber) + beatWithinMeasure));
        this.measureNumber = measureNumber;
        this.beatWithinMeasure = beatWithinMeasure;
    }

    public double measureNumber() {
        return this.measureNumber + this.beatWithinMeasure;
    }

    public int beatNumber() {
        return this.beatNumber;
    }

    public void setBeatNumber(short beatNumber) {
        this.beatNumber = beatNumber;
    }

    public int startTicks() {
        return this.beatStartTicks;
    }

    public int endTicks() {
        return this.beatEndTicks;
    }

    public void addScore(Instrument instrument,
                         int score) {
        //System.out.println("beat " + this.measureNumber() + " adding " + score);
        this.score += score;
        if (Instrument.DRUMS.equals(instrument)) {
            this.drumScore += score;
        }

        if (Instrument.VOCALS.equals(instrument)) {
            this.vocalScore += score;
        }

        if (this.score < 0) {
            System.out.println("beat " + this.measureNumber() + " negative score?!? added " + score);
        }
    }

    public void setWhammy(Instrument instrument, byte whammy) {
        //System.out.println("beat " + this.measureNumber() + " whammy " + whammy);
        this.whammy[instrument.index()] = whammy;
    }

    public short maximumOverdrive(Instrument instrument) {
        return this.maximumOverdriveBar[instrument.index()];
    }

    public void setIsDrumFill(boolean isDrumFill) {
        this.isDrumFill = isDrumFill;
    }

    public boolean isDrumFill() {
        return this.isDrumFill;
    }

    public void addDuration(int duration) {
        this.duration += duration;
    }

    public int duration() {
        return this.duration;
    }

    public boolean instrumentCanActivate(Instrument instrument) {
        return this.instrumentCanActivate(instrument.index());
    }

    public boolean instrumentCanActivate(int instrumentIndex) {
        return (this.instrumentCanActivate[instrumentIndex]);
    }

    public boolean instrumentCanActivate(Instrument instrument, BandState bandState) {
        return this.instrumentCanActivate(instrument.index(), bandState);
    }

    public boolean instrumentCanActivate(int instrumentIndex, BandState bandState) {
        return ((!bandState.instrumentInOverdrive(instrumentIndex)) &&
                (this.instrumentCanActivate[instrumentIndex]) && 
                (bandState.getInstrumentMeter(instrumentIndex) >= SongInfo.OVERDRIVE_HALFBAR));
    }

    public void setInstrumentCanActivate(Instrument instrument, boolean value) {
        this.setInstrumentCanActivate(instrument.index(), value);
    }

    public void setInstrumentCanActivate(int instrument, boolean value) {
        this.instrumentCanActivate[instrument] = value;
    }

    public void setReachableMeter(Instrument instrument,
                                  int meter) {
        this.isReachableActiveMeter[instrument.index()][meter] = true;
    }

    public boolean isReachableActiveMeter(Instrument instrument,
                                    int meter) {
        return this.isReachableActiveMeter[instrument.index()][meter];
    }

    public boolean hasSqueezeAvailable(Instrument instrument) {
        return false;
    }

    public boolean hasWhammy(Instrument instrument) {
        return this.getWhammy(instrument) > 0;
    }

    public byte getWhammy(Instrument instrument) {
        return this.getWhammy(instrument.index());
    }

    public byte getWhammy(int instrument) {
        return this.whammy[instrument];
    }

    public void setOverdrivePhraseEnd(Instrument instrument, boolean phraseEnd) {
        //System.out.println("beat " + this.measureNumber() + " phrase end");
        this.hasOverdrivePhraseEnd[instrument.index()] = phraseEnd;
    }

    public boolean hasOverdrivePhraseEnd(int instrument) {
        return this.hasOverdrivePhraseEnd[instrument];
    }

    public boolean hasOverdrivePhraseEnd(Instrument instrument) {
        return this.hasOverdrivePhraseEnd(instrument.index());
    }

    public void setLastOverdriveNote(Instrument instrument, boolean lastNote) {
        //System.out.println("beat " + this.measureNumber() + " last od note");
        this.hasLastOverdriveNote[instrument.index()] = true;
    }

    public boolean hasLastOverdriveNote(int instrument) {
        return this.hasLastOverdriveNote[instrument];
    }

    public boolean hasLastOverdriveNote(Instrument instrument) {
        return this.hasLastOverdriveNote(instrument.index());
    }

    public boolean isVocalPhraseEnd() {
        return this.isVocalPhraseEnd;
    }

    public void setVocalPhraseEnd(boolean value) {
        this.isVocalPhraseEnd = true;
    }

    public void setLastBeatOfUnisonBonus(boolean value) {
        this.hasLastBeatOfUnisonBonus = true;
    }

    public void setODToFillDuration(int duration) {
        this.odToFillDuration = duration;
    }

    public int odToFillDuration() {
        return this.odToFillDuration;
    }

    public int score(BandState startState, BandState endState) {
        int result = this.score;

        // subtract out the drum score if this is part of an overdrive fill
        // needs to take into account distance to last OD activation

        if (startState.instrumentCanActivate(Instrument.DRUMS) && 
            this.isDrumFill()) {

            // if the fill is too close to the most recent OD the fill is
            // uncovered
            //
            // (this doesn't work if a unison bonus phrase causes the half-bar
            // slightly later ... need to update odToFillDuration during
            // computeUnisonBonus
            if ((SongInfo.OVERDRIVE_HALFBAR == 
                 startState.getInstrumentMeter(Instrument.DRUMS)) &&
                this.odToFillDuration() < DrumTrackHandler.MIN_FILL_DURATION) {
                result += drumScore;
            }

            result -= this.drumScore;
        }

        int instrumentsInOverdrive = 0;
        // Simulating a beat occurs in this order
        // (1) toggle activations
        // (2) play through beat
        // (3) deactivate any fully drained OD

        // therefore, the beat was played in OD if either the start or end state
        // occured during overdrive


        for (int i = 0; i < 4; ++i) {
            if (startState.instrumentInOverdrive(i) ||
                endState.instrumentInOverdrive(i)) {
                instrumentsInOverdrive++;
            }
        }

        int multiplier = computeMultiplier(instrumentsInOverdrive);
        boolean phraseEndAndActivation =
            this.isVocalPhraseEnd() && 
            startState.instrumentInOverdrive(Instrument.VOCALS) && 
            endState.instrumentInOverdrive(Instrument.VOCALS);

        if (phraseEndAndActivation) {
            int vocalInstrumentsInOverdrive = instrumentsInOverdrive - 1;
            int vocalMultiplier = computeMultiplier(vocalInstrumentsInOverdrive);
            result -= vocalScore;
            return vocalMultiplier*this.vocalScore + result*multiplier;
        }
 

        return result * multiplier;
    }

    public int score(BandState bandState) {
        int result = this.score;

        // subtract out the drum score if this is part of an overdrive fill
        // needs to take into account distance to last OD activation

        if (bandState.instrumentCanActivate(Instrument.DRUMS)) {
            result -= this.drumScore;
        }

        int instrumentsInOverdrive = 0;
        for (int i = 0; i < 4; ++i) {
            instrumentsInOverdrive += bandState.instrumentInOverdrive(i) ? 1 : 0;
        }

        // this is devious.
        // The first term here is straight forward. Just 2x # instruments
        // The second term is a 0-1 variable that is 0 if and only if
        // instruments == 0. So with nothing in overdrive, it reduces to
        //      0 + 1 = 1
        // But for all other cases it reduces to
        //      2 * N + 0 == 2* N

        // assert(instruments <=4, "more than 4 instruments in overdrive");
        int multiplier = 2*instrumentsInOverdrive + (1-(instrumentsInOverdrive+3)/4);

        return result * multiplier;
    }

    private static int computeMultiplier(int instrumentsInOverdrive) {
        // this is devious.
        // The first term here is straight forward. Just 2x # instruments
        // The second term is a 0-1 variable that is 0 if and only if
        // instruments == 0. So with nothing in overdrive, it reduces to
        //      0 + 1 = 1
        // But for all other cases it reduces to
        //      2 * N + 0 == 2* N

        // assert(instruments <=4, "more than 4 instruments in overdrive");
        return 2*instrumentsInOverdrive + (1-(instrumentsInOverdrive+3)/4);
    }


    private byte findMaxmimumActiveMeter(int instrument) {
        for (byte i = maximumOverdriveBar[instrument]; i >= 0; --i) {
            if (this.isReachableActiveMeter[instrument][i]) {
                return i;
            }
        }

        System.out.println("no reachable active meter?!?");
        return 0;
    }

    public void computeReachableStates(Collection< BandState > results) throws Exception {
        // start with "no one active, everyone with current maximumOverdriveBar"
        // this state is always theortically reachable simply by never
        // activating
        BandState firstBandState = new BandState();
        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            firstBandState.setInstrumentInOverdrive(i, false);
            firstBandState.setInstrumentMeter(i,this.maximumOverdriveBar[i]);
        }
        results.add(firstBandState);
        
        // toggle activations. Now we may have as many as 16 states. Some of
        // these may be unreachable (e.g. near the end of verse three of
        // "Where'd You Go" vocals must be inactive), but that's okay; the final
        // forward walk will ignore these.

        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(16);
            for (BandState tmpState : results) {
                if ((this.maximumOverdriveBar[i] >= SongInfo.OVERDRIVE_HALFBAR) &&
                    this.canBeInOverdrive[i]) {
                    // find the maximum active overdrive amount
                    // usually OVERDRIVE_FULLBAR for guitar/bass

                    BandState newState = (BandState) (tmpState.clone());
                    byte activeMeter = this.findMaxmimumActiveMeter(i);
                    newState.setInstrumentMeter(i, activeMeter);
                    newState.activateInstrument(i);
                    tmp.add(newState);
                }
            }
            results.addAll(tmp);
        }

        // first the easy part: iterate over all possible amounts of overdrive
        // for guitar and bass in [0,maximumOverdriveBar)
        //
        // technically, this will compute some unreachable states, (e.g. guitar
        // with 23 beats if that amount of whammy is unattainable at certain
        // times in the song. Guitar with 14 beats 1 beat after first possible
        // activation, etc.), but that's okay ... the forward walk will prevent
        // us from encountering those values since they won't be reachable when
        // we start from the constrained space where no one has enough OD to
        // activate.
        {
            ArrayList< BandState >  tmp = new ArrayList< BandState >(results.size());
            for (byte i = (byte) (this.maximumOverdriveBar[Instrument.GUITAR.index()]-1); i >= 0; --i) {
                for (BandState tmpState : results) {
                    if (0 == i && tmpState.instrumentInOverdrive(Instrument.GUITAR)) {
                        continue;
                    }
                    BandState newState = (BandState) (tmpState.clone());
                    newState.setInstrumentMeter(Instrument.GUITAR.index(), i);
                    tmp.add(newState);
                }
            }
            results.addAll(tmp);
        }

        {
            ArrayList< BandState >  tmp = new ArrayList< BandState >(results.size());
            for (byte i = (byte) (this.maximumOverdriveBar[Instrument.BASS.index()]-1); i >= 0; --i) {
                for (BandState tmpState : results) {
                    if (0 == i && tmpState.instrumentInOverdrive(Instrument.BASS)) {
                        continue;
                    }
                    BandState newState = (BandState) (tmpState.clone());
                    newState.setInstrumentMeter(Instrument.BASS.index(), i);
                    tmp.add(newState);
                }
            }
            results.addAll(tmp);
        }


        // now possibly up to 16*65^2 = 67600 states

        // now for vox and drums
        // 
        // for these instruments, we only want to add a state if we are certain
        // it's actually possible to have that amount of OD
        {
            ArrayList< BandState >  tmp = new ArrayList< BandState >(results.size());
            for (byte i = (byte) (this.maximumOverdriveBar[Instrument.DRUMS.index()]-1); i >= 0; --i) {

                for (BandState tmpState : results) {
                    // if the drums are in overdrive, skip meters that are
                    // unreachable
                    if (tmpState.instrumentInOverdrive(Instrument.DRUMS)) {
                        if (!this.isReachableActiveMeter(Instrument.DRUMS, i)) {
                            continue;
                        }
                    } else {
                    // if we're NOT in overdrive, skip meters that aren't
                    // multiples of a phrase
                        if (0 != (i % SongInfo.OVERDRIVE_PHRASE)) {
                            continue;
                        }
                    }
                    BandState newState = (BandState) (tmpState.clone());
                    newState.setInstrumentMeter(Instrument.DRUMS.index(), i);
                    tmp.add(newState);
                }
            }
            results.addAll(tmp);
        }

        {
            ArrayList< BandState >  tmp = new ArrayList< BandState >(results.size());
            for (byte i = (byte) (this.maximumOverdriveBar[Instrument.VOCALS.index()]-1); i >= 0; --i) {
                for (BandState tmpState : results) {
                    if (tmpState.instrumentInOverdrive(Instrument.VOCALS)) {
                        if (!this.isReachableActiveMeter(Instrument.VOCALS, i)) {
                            continue;
                        }
                    } else {
                        if (0 != (i % SongInfo.OVERDRIVE_PHRASE)) {

                            continue;
                        }
                    }

                    BandState newState = (BandState) (tmpState.clone());
                    newState.setInstrumentMeter(Instrument.VOCALS.index(), i);
                    tmp.add(newState);
                }
            }
            results.addAll(tmp);
        }

        // unclear on how many states we are talking about here. For drums the
        // typical measure will have about 9 reachable states (off-beat
        // activations will make this number larger). For vocals, verses and
        // chorus are usually unable to activate more than half the time. But
        // sometimes there are long sections with no singing or taps. Assume it
        // averages out to 33 vocal states, we're probably near 20M states
        // per beat
    }

    public void computeReachableNextStates(BandState currentBandState, Collection< BandState > results) throws Exception {
        //
        BandState initialBandState = (BandState) currentBandState.clone();
        boolean canSqueezeDrums = initialBandState.canSqueeze(Instrument.DRUMS, this);

        results.add(initialBandState);

        // toggle activations for any instrument that is in a position to do so
        ArrayList< BandState > preActivationStates = new ArrayList< BandState > (results.size());
        preActivationStates.addAll(results);

        for (BandState tmpState : preActivationStates) {
            for(int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
                if (this.instrumentCanActivate(i, tmpState)) {
                    ArrayList< BandState > tmp = new ArrayList< BandState > (results.size());
                    for (BandState cur : results) {
                        BandState newBandState  = (BandState) cur.clone();
                        newBandState.activateInstrument(i);
                        tmp.add(newBandState);
                    }
                    results.addAll(tmp);
                }
            }
        }

        // apply whammy (does not affect activation)

        if (this.hasWhammy(Instrument.GUITAR)) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());
            for (BandState tmpState : results) {
                // skip the whammy if there's no room for it
                if (!tmpState.instrumentInOverdrive(Instrument.GUITAR) &&
                    SongInfo.OVERDRIVE_FULLBAR == tmpState.getInstrumentMeter(Instrument.GUITAR)) {
                    continue;
                }

                BandState newBandState = (BandState) tmpState.clone();
                newBandState.applyWhammy(Instrument.GUITAR, this);
                tmp.add(newBandState);
            }

            results.addAll(tmp);
        }

        if (this.hasWhammy(Instrument.BASS)) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());
            for (BandState tmpState : results) {
                if (!tmpState.instrumentInOverdrive(Instrument.BASS) &&
                    SongInfo.OVERDRIVE_FULLBAR == tmpState.getInstrumentMeter(Instrument.BASS)) {
                    continue;
                }

                BandState newBandState = (BandState) tmpState.clone();
                newBandState.applyWhammy(Instrument.BASS, this);
                tmp.add(newBandState);
            }

            results.addAll(tmp);
        }

        // acquire overdrive phrases (does not truncate)
        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            if (this.hasLastOverdriveNote(i)) {
                for (BandState tmpState : results) {
                    BandState newBandState = (BandState) tmpState.clone();
                    tmpState.acquireOverdrive(i);
                }
            }
        }

        if (this.hasLastBeatOfUnisonBonus()) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());
            for (BandState tmpState: results) {
                tmpState.acquireUnisonBonus();
            }

        }


        // advance all the instruments that are in overdrive (affects
        // activation)
        {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());

            for (BandState tmpState : results) {

                if (canSqueezeDrums) {
                    System.out.println("squeezing drums");
                    BandState squeezedBandState = (BandState) tmpState.clone();
                    squeezedBandState.advanceActivatedInstruments(this, true);
                    tmp.add(squeezedBandState);
                }
                tmpState.advanceActivatedInstruments(this, false);
            }

            results.addAll(tmp);
        }



        // assert(results.size() < 128
    }

    public boolean hasLastBeatOfUnisonBonus() {
        return this.hasLastBeatOfUnisonBonus;
    }


    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Beat Count: ");
        result.append(this.beatNumber);
        result.append("  Measure Count: ");
        result.append(((double) this.measureNumber) + this.beatWithinMeasure);
        result.append("  Start ticks: ");
        result.append(this.beatStartTicks);
        result.append("  End ticks: ");
        result.append(this.beatEndTicks);
        result.append("  Vox end: ");
        result.append(this.isVocalPhraseEnd ? "Y" : "N");
        result.append("\n Score:");
        result.append(this.score);
        result.append("  Fill Score: ");
        result.append(this.isDrumFill() ? this.drumScore : 0);
        result.append("  Whammy: ");
        result.append(this.whammy[Instrument.GUITAR.index()]);
        result.append("|");
        result.append(this.whammy[Instrument.BASS.index()]);
        result.append("  Duration: ");
        result.append(this.duration);
        result.append("  OD->Fill: ");
        result.append(this.odToFillDuration);
        result.append("\n OD last note: ");
        result.append(this.hasLastOverdriveNote[Instrument.GUITAR.index()] ?  "Y" : "N" );
        result.append("|");
        result.append(this.hasLastOverdriveNote[Instrument.DRUMS.index()] ? "Y" : "N" );
        result.append("|");
        result.append(this.hasLastOverdriveNote[Instrument.VOCALS.index()] ? "Y" : "N" );
        result.append("|");
        result.append(this.hasLastOverdriveNote[Instrument.BASS.index()] ? "Y" : "N" );
        result.append("  OD phrase end: ");
        result.append(this.hasOverdrivePhraseEnd[Instrument.GUITAR.index()] ?  "Y" : "N" );
        result.append("|");
        result.append(this.hasOverdrivePhraseEnd[Instrument.DRUMS.index()] ? "Y" : "N" );
        result.append("|");
        result.append(this.hasOverdrivePhraseEnd[Instrument.VOCALS.index()] ? "Y" : "N" );
        result.append("|");
        result.append(this.hasOverdrivePhraseEnd[Instrument.BASS.index()] ? "Y" : "N" );
        result.append("  Max OD: ");
        result.append(this.maximumOverdriveBar[Instrument.GUITAR.index()]);
        result.append("|");
        result.append(this.maximumOverdriveBar[Instrument.DRUMS.index()]);
        result.append("|");
        result.append(this.maximumOverdriveBar[Instrument.VOCALS.index()]);
        result.append("|");
        result.append(this.maximumOverdriveBar[Instrument.BASS.index()]);

        result.append("\n Activations: Drums: ");
        result.append(this.instrumentCanActivate[Instrument.DRUMS.index()] ? "Y" : "N");
        result.append(" Vocals: ");
        result.append(this.instrumentCanActivate[Instrument.VOCALS.index()] ? "Y" : "N");

        result.append(" Reachable Meters:\n  Drums:");
        for (int i = 0; i < this.isReachableActiveMeter[Instrument.DRUMS.index()].length; ++i) {
            result.append(this.isReachableActiveMeter[Instrument.DRUMS.index()][i] ?  "Y" : "N");
        }

        result.append("\n  Vocal:");
        for (int i = 0; i < this.isReachableActiveMeter[Instrument.VOCALS.index()].length; ++i) {
            result.append(this.isReachableActiveMeter[Instrument.VOCALS.index()][i] ?  "Y" : "N");
        }


        // whammy? activation locations?

        return result.toString();
    }

    public static BeatInfo fromPathStats(short beatNumber,
                                         String currentBeat,
                                         String nextBeat) {
        BeatInfo result = new BeatInfo();
        result.beatNumber = beatNumber;

        StringTokenizer tok = new StringTokenizer (currentBeat, "\t");
        double parsedBeatNumber = Double.valueOf(tok.nextToken()).doubleValue();
        result.measureNumber = (short) parsedBeatNumber;
        result.beatWithinMeasure = parsedBeatNumber - ((double) result.measureNumber);

        String dummy1 = tok.nextToken(); // skip the beat count
        String dummy2 = tok.nextToken(); // skip the OD notes
        result.score = Short.valueOf(tok.nextToken()).shortValue();
        result.drumScore = Short.valueOf(tok.nextToken()).shortValue();

        result.instrumentCanActivate[Instrument.DRUMS.index()] = currentBeat.contains("drum act");
        result.instrumentCanActivate[Instrument.VOCALS.index()] = currentBeat.contains("vox tacet");

        result.hasOverdrivePhraseEnd[Instrument.BASS.index()] = nextBeat.contains("bass od");
        result.hasOverdrivePhraseEnd[Instrument.VOCALS.index()] = nextBeat.contains("vox od");
        result.hasOverdrivePhraseEnd[Instrument.DRUMS.index()] = nextBeat.contains("drum od");
        result.hasOverdrivePhraseEnd[Instrument.GUITAR.index()] = nextBeat.contains("guitar od");

        if (nextBeat.contains("unison bonus")) {
            result.hasOverdrivePhraseEnd[Instrument.GUITAR.index()] = 
                result.hasOverdrivePhraseEnd[Instrument.BASS.index()] =
                result.hasOverdrivePhraseEnd[Instrument.DRUMS.index()] = 
                result.hasLastBeatOfUnisonBonus = true;
        }
        
        return result;
    }

}
