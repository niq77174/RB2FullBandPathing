package com.scorehero.pathing.fullband;

import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

public class BeatInfo implements Cloneable {
    short beatNumber;
    short measureNumber;
    double beatWithinMeasure; // between 0.0 and 1.0
    short score;
    short drumScore; // 0 if not covered by fill
    boolean hasLastBeatOfInstrumentOverdrive[];
    boolean hasLastBeatOfUnisonBonus;
    boolean instrumentCanActivate[];
    byte maximumOverdriveBar[];

    boolean canBeInOverdrive[];
    boolean isReachableActiveMeter[][];
    short whammy[];

    public BeatInfo() {
        this.hasLastBeatOfInstrumentOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentCanActivate = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentCanActivate[Instrument.GUITAR.index()] =
        this.instrumentCanActivate[Instrument.BASS.index()] = true;

        this.canBeInOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.canBeInOverdrive[Instrument.GUITAR.index()] = true;
        this.canBeInOverdrive[Instrument.BASS.index()] = true;

        this.maximumOverdriveBar = new byte[Instrument.INSTRUMENT_COUNT.index()];

        this.isReachableActiveMeter = new boolean[Instrument.INSTRUMENT_COUNT.index()][SongInfo.OVERDRIVE_FULLBAR+1];
        for (int i = 1; i < SongInfo.OVERDRIVE_FULLBAR + 1; ++i) {
            this.isReachableActiveMeter[Instrument.GUITAR.index()][i] =
            this.isReachableActiveMeter[Instrument.BASS.index()][i] = true;
        }
        this.whammy = new short[Instrument.INSTRUMENT_COUNT.index()];
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
            System.out.println("whammying guitar");
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
            System.out.println("whammying bass");
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
            if (this.hasOverdrivePhraseEnd(i)) {
                for (BandState tmpState : results) {
                    BandState newBandState = (BandState) tmpState.clone();
                    tmpState.acquireOverdrive(i);
                }
            }
        }

        if (this.hasUnisonBonusPhraseEnd()) {
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

    public short maximumOverdrive(Instrument instrument) {
        return this.maximumOverdriveBar[instrument.index()];
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

    public short getWhammy(Instrument instrument) {
        return this.whammy[instrument.index()];
    }

    public short score(BandState startState, BandState endState) {
        short result = this.score;

        // subtract out the drum score if this is part of an overdrive fill
        // needs to take into account distance to last OD activation

        if (startState.instrumentCanActivate(Instrument.DRUMS)) {
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

        // this is devious.
        // The first term here is straight forward. Just 2x # instruments
        // The second term is a 0-1 variable that is 0 if and only if
        // instruments == 0. So with nothing in overdrive, it reduces to
        //      0 + 1 = 1
        // But for all other cases it reduces to
        //      2 * N + 0 == 2* N

        // assert(instruments <=4, "more than 4 instruments in overdrive");
        short multiplier = (short)
            (2*instrumentsInOverdrive + (1-(instrumentsInOverdrive+2)/3));

        return (short) (result * multiplier);
    }

    public short score(BandState bandState) {
        short result = this.score;

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
        short multiplier = (short)
            (2*instrumentsInOverdrive + (1-(instrumentsInOverdrive+2)/3));

        return (short) (result * multiplier);
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

        result.hasLastBeatOfInstrumentOverdrive[Instrument.BASS.index()] = nextBeat.contains("bass od");
        result.hasLastBeatOfInstrumentOverdrive[Instrument.VOCALS.index()] = nextBeat.contains("vox od");
        result.hasLastBeatOfInstrumentOverdrive[Instrument.DRUMS.index()] = nextBeat.contains("drum od");
        result.hasLastBeatOfInstrumentOverdrive[Instrument.GUITAR.index()] = nextBeat.contains("guitar od");

        if (nextBeat.contains("unison bonus")) {
            result.hasLastBeatOfInstrumentOverdrive[Instrument.GUITAR.index()] = 
                result.hasLastBeatOfInstrumentOverdrive[Instrument.BASS.index()] =
                result.hasLastBeatOfInstrumentOverdrive[Instrument.DRUMS.index()] = 
                result.hasLastBeatOfUnisonBonus = true;
        }
        
        return result;
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

    public boolean hasOverdrivePhraseEnd(int instrument) {
        return this.hasLastBeatOfInstrumentOverdrive[instrument];
    }

    public boolean hasOverdrivePhraseEnd(Instrument instrument) {
        return this.hasOverdrivePhraseEnd(instrument.index());
    }

    public boolean hasUnisonBonusPhraseEnd() {
        return this.hasLastBeatOfUnisonBonus;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Overall Beat Number: ");
        result.append(this.beatNumber);
        result.append("  Measure/Beat number: ");
        result.append(((double) this.measureNumber) + this.beatWithinMeasure);
        result.append("\n Score:");
        result.append(this.score);
        result.append(", Drum Score: ");
        result.append(this.drumScore);
        result.append("\n Overdrive Phrase: Guitar:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.GUITAR.index()]);
        result.append(", Drums:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.DRUMS.index()]);
        result.append(", Vocals:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.VOCALS.index()]);
        result.append(", Bass:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.BASS.index()]);
        result.append("\n Maximum Overdrive: Guitar:");
        result.append(this.maximumOverdriveBar[Instrument.GUITAR.index()]);
        result.append(", Drums:");
        result.append(this.maximumOverdriveBar[Instrument.DRUMS.index()]);
        result.append(", Vocals:");
        result.append(this.maximumOverdriveBar[Instrument.VOCALS.index()]);
        result.append(", Bass:");
        result.append(this.maximumOverdriveBar[Instrument.BASS.index()]);

        result.append("\n Activations: Drums: ");
        result.append(this.instrumentCanActivate[Instrument.DRUMS.index()] ? 'Y' : 'N');
        result.append(" Vocals: ");
        result.append(this.instrumentCanActivate[Instrument.VOCALS.index()] ? 'Y' : 'N');

        result.append(" Reachable Meters:\n  Drums:");
        for (int i = 0; i < this.isReachableActiveMeter[Instrument.DRUMS.index()].length; ++i) {
            result.append(this.isReachableActiveMeter[Instrument.DRUMS.index()][i] ?  "Y," : "N,");
        }

        result.append("\n  Vocal:");
        for (int i = 0; i < this.isReachableActiveMeter[Instrument.VOCALS.index()].length; ++i) {
            result.append(this.isReachableActiveMeter[Instrument.VOCALS.index()][i] ?  "Y," : "N,");
        }


        // whammy? activation locations?

        return result.toString();
    }

}
