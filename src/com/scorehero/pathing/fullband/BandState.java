package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;

public class BandState {
    double guitarWhammyPartialBeat; // between 0.0 and 1.0;
    double bassWhammyPartialBeat; // between 0.0 and 1.0;
    short instrumentMeter[];
    boolean instrumentInOverdrive[];

    public BandState() {
        this.instrumentInOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentMeter = new short[Instrument.INSTRUMENT_COUNT.index()];
    }

    public void computeReachableStatesForNextBeat(SongInfo songInfo,
                                                  int currentBeatIndex,
                                                  Collection< BandState > bandStates) throws Exception {
        BeatInfo currentBeat = songInfo.beats().get(currentBeatIndex);
        ArrayList< BandState > result = new ArrayList< BandState >();
        result.add((BandState) this.clone());

        // add on any overdrive for this measure
        for (BandState bandState : result) {
            for (int i = 0; i < 4; ++i) {
                if (currentBeat.hasLastBeatOfInstrumentOverDrive[i]) {
                    bandState.instrumentMeter[i] += 8 * SongInfo.SUBBEATS_PER_BEAT;
                }
            }

            if (currentBeat.hasLastBeatOfUnisonBonus) {
                bandState.instrumentMeter[Instrument.GUITAR.index()] += 8 * SongInfo.SUBBEATS_PER_BEAT;
                bandState.instrumentMeter[Instrument.DRUMS.index()] += 8 * SongInfo.SUBBEATS_PER_BEAT;
                bandState.instrumentMeter[Instrument.BASS.index()] += 8 * SongInfo.SUBBEATS_PER_BEAT;
            }
        }

        double beatGuitarWhammy = 1.1 * currentBeat.guitarWhammy;
        
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

        double beatBassWhammy = 1.1 * currentBeat.bassWhammy;
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
                    bandState.instrumentMeter[i] =
                        (short) Math.min(bandState.instrumentMeter[i], 32*SongInfo.SUBBEATS_PER_BEAT);
                }
            }
        }

        // time to check who can activate!
        ArrayList< BandState > finalResult = new ArrayList< BandState >(result);
        BeatInfo nextBeat = songInfo.beats().get(currentBeatIndex+1);
        for (BandState bandState : result) {
            for (int i = 0; i < 4; ++i) {
                ArrayList< BandState > tmp = new ArrayList< BandState >(finalResult);
                if (nextBeat.instrumentCanActivate[i] && bandState.instrumentMeter[i] > 16*SongInfo.SUBBEATS_PER_BEAT) {
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
}
