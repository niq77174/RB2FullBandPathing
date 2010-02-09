package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;

public class BandState implements Cloneable {
    private double guitarWhammyPartialBeat; // between 0.0 and 1.0;
    private double bassWhammyPartialBeat; // between 0.0 and 1.0;
    private short instrumentMeter[];
    private boolean instrumentInOverdrive[];

    public BandState() {
        this.instrumentInOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentMeter = new short[Instrument.INSTRUMENT_COUNT.index()];
    }

    // this logic should really go in the SongInfo class,
    public void computeReachableStatesForNextBeat(SongInfo songInfo,
                                                  int currentBeatIndex,
                                                  Collection< BandState > bandStates) throws Exception {
        BeatInfo currentBeat = songInfo.beats().get(currentBeatIndex);
        ArrayList< BandState > result = new ArrayList< BandState >();
        result.add((BandState) this.clone());

        // add on any overdrive for this measure
        for (BandState bandState : result) {
            for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
                if (currentBeat.hasLastBeatOfInstrumentOverdrive[i]) {
                    bandState.instrumentMeter[i] += SongInfo.OVERDRIVE_PHRASE;
                }
            }

            if (currentBeat.hasLastBeatOfUnisonBonus) {
                bandState.instrumentMeter[Instrument.GUITAR.index()] += SongInfo.OVERDRIVE_PHRASE;
                bandState.instrumentMeter[Instrument.DRUMS.index()] += SongInfo.OVERDRIVE_PHRASE;
                bandState.instrumentMeter[Instrument.BASS.index()] += SongInfo.OVERDRIVE_PHRASE;
            }
        }

        double beatGuitarWhammy = 1.088 * currentBeat.guitarWhammy;
        
        // guitar: to whammy? or not to whammy?
        if (beatGuitarWhammy > 0.0) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(result);
            for (BandState bandState : tmp) {
                BandState newBandState = (BandState) bandState.clone();
                newBandState.guitarWhammyPartialBeat += 
                    beatGuitarWhammy;
                newBandState.instrumentMeter[Instrument.GUITAR.index()] += 
                    Math.floor(newBandState.guitarWhammyPartialBeat);
                newBandState.guitarWhammyPartialBeat -= 
                    Math.floor(newBandState.guitarWhammyPartialBeat);
                result.add(newBandState);
            }
        }
        // assert(result.size()) <= 2

        double beatBassWhammy = 1.088 * currentBeat.bassWhammy;
        // bass: to whammy? or not to whammy?
        if (beatBassWhammy > 0.0) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(result);
            for (BandState bandState : tmp) {
                BandState newBandState = (BandState) bandState.clone();
                newBandState.bassWhammyPartialBeat += 
                    beatBassWhammy;
                newBandState.instrumentMeter[Instrument.BASS.index()] += 
                    Math.floor(newBandState.bassWhammyPartialBeat);
                newBandState.bassWhammyPartialBeat -= 
                    Math.floor(newBandState.bassWhammyPartialBeat);
                result.add(newBandState);
            }
        }
        // assert(result.size()) <= 4

        // decrement the OD meter for every instrument that is in overdrive
        // also cap overdrive meter to 32*subbeats/beat
        for (BandState bandState : result) {
            for (int i = 0; i < 4; ++i) {
                if (bandState.instrumentInOverdrive[i]) {
                    // check for partial beats, &c.
                    bandState.instrumentMeter[i]--;
                    if (0 == bandState.instrumentMeter[i]) {
                        bandState.instrumentInOverdrive[i] = false;
                    }
                }
            }

            Util.truncateOverdriveMeters(bandState.instrumentMeter);
        }

        // time to check who can activate!
        ArrayList< BandState > finalResult = new ArrayList< BandState >(result);
        BeatInfo nextBeat = songInfo.beats().get(currentBeatIndex+1);
        for (BandState bandState : result) {
            for (int i = 0; i < 4; ++i) {
                ArrayList< BandState > tmp = new ArrayList< BandState >(finalResult);
                if (nextBeat.instrumentCanActivate(i, bandState)) {
                    for (BandState tmpBandState : tmp) {
                        BandState newBandState = (BandState) tmpBandState.clone();
                        newBandState.instrumentInOverdrive[i] = true;

                        if (Instrument.GUITAR.index() == i) {
                            newBandState.guitarWhammyPartialBeat = 0.0;
                        }
                        if (Instrument.BASS.index() == i) {
                            newBandState.bassWhammyPartialBeat = 0.0;
                        }
                        finalResult.add(newBandState);
                    }
                }
            }
        }

        // assert(result.size()) <= 64
        bandStates.addAll(result);
    }

    public int serializedData() {
        return 0; //
    }

    public  static BandState fromSerializedData(int value) {
        return null; // TODO
    }

    public void applyWhammy(Instrument instrument, BeatInfo beatInfo) {
    }

    public boolean canSqueeze(Instrument instrument, BeatInfo beatInfo) {
        return false;
    }

    public void whammyGuitar() {
        this.guitarWhammyPartialBeat += 1.088;
        this.instrumentMeter[Instrument.GUITAR.index()] += Math.floor(this.guitarWhammyPartialBeat);
        this.guitarWhammyPartialBeat -= Math.floor(this.guitarWhammyPartialBeat);
        //assert(this.guitarWhammyPartialBeat < 1.0)
    }

    public void whammyBass() {
        this.bassWhammyPartialBeat += 1.088;
        this.instrumentMeter[Instrument.BASS.index()] += Math.floor(this.bassWhammyPartialBeat);
        this.bassWhammyPartialBeat -= Math.floor(this.bassWhammyPartialBeat);
        //assert(this.bassWhammyPartialBeat < 1.0)
    }

    public void advanceActivatedInstruments(BeatInfo beatInfo, boolean squeezeDrums) {
        for (int i = 0; i < this.instrumentMeter.length; ++i) {
            if (this.instrumentInOverdrive(i)) {
                --this.instrumentMeter[i];
            }
        }
    }

    public void activateInstrument(int instrument) {
        this.instrumentInOverdrive[instrument] = true;
    }

    public void acquireUnisonBonus() {
        this.instrumentMeter[Instrument.GUITAR.index()] += SongInfo.OVERDRIVE_PHRASE;
        this.instrumentMeter[Instrument.BASS.index()] += SongInfo.OVERDRIVE_PHRASE;
        this.instrumentMeter[Instrument.DRUMS.index()] += SongInfo.OVERDRIVE_PHRASE;
    }

    public void drainOverdrive(Instrument instrument) {
        --this.instrumentMeter[instrument.index()];
        //assert(this.instrumentMeter[instrumentMeter.index()] > 0);
    }

    public void acquireInstrumentOverdrive(Instrument instrument) {
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("BandState: Instrument Meter: ");
        result.append("Guitar: " + instrumentMeter[Instrument.GUITAR.index()]);
        result.append(", Drums: " + instrumentMeter[Instrument.DRUMS.index()]);
        result.append(", Vocals: " + instrumentMeter[Instrument.VOCALS.index()]);
        result.append(", Bass: " + instrumentMeter[Instrument.BASS.index()]);
        result.append("\n instruments in overdrive: ");
        result.append("Guitar: " + (instrumentInOverdrive[Instrument.GUITAR.index()] ? 'Y' : 'N'));
        result.append(", Drums: " + (instrumentInOverdrive[Instrument.DRUMS.index()] ? 'Y' : 'N'));
        result.append(", Vocals: " + (instrumentInOverdrive[Instrument.VOCALS.index()] ? 'Y' : 'N'));
        result.append(", Bass: " + (instrumentInOverdrive[Instrument.BASS.index()] ? 'Y' : 'N'));
        return result.toString();
    }

    public void setInstrumentInOverdrive(int instrument, boolean value) {
        this.instrumentInOverdrive[instrument] = value;
    }

    public void setInstrumentMeter(int instrument, short value) {
        this.instrumentMeter[instrument] = value;
    }

    public int getInstrumentMeter(int instrument) {
        return this.instrumentMeter[instrument];
    }

    public boolean instrumentInOverdrive(int instrument) {
        return this.instrumentInOverdrive[instrument];
    }

    public Object clone() throws CloneNotSupportedException {
        BandState result = (BandState) super.clone();
        result.instrumentInOverdrive = this.instrumentInOverdrive.clone();
        result.instrumentMeter = this.instrumentMeter.clone();
        return result;
    }

}
