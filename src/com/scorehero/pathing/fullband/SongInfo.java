package com.scorehero.pathing.fullband;

import java.util.ArrayList;
import java.util.Collection;
import java.io.LineNumberReader;
import java.io.FileReader;

public class SongInfo {
    private ArrayList< BeatInfo > beats;

    public final static int SUBBEATS_PER_BEAT = 1 ; // will be larger for some songs
    public final static int OVERDRIVE_PHRASE = 8 * SUBBEATS_PER_BEAT; 
    public final static int OVERDRIVE_HALFBAR = 2 * OVERDRIVE_PHRASE;
    public final static int OVERDRIVE_FULLBAR = 4 * OVERDRIVE_PHRASE;

    // not always song-level data :/
    byte beatsPerMeasure;
    byte lengthOfBeat;

    public ArrayList< BeatInfo > beats() {
        return this.beats;
    }

    public static SongInfo fromPathStatsFile(String fileName) throws Exception{
        SongInfo result = new SongInfo();
        result.beats = new ArrayList< BeatInfo >();
        LineNumberReader in = new LineNumberReader(new FileReader(fileName));
        String currentLine = in.readLine();
        String nextLine = in.readLine();
        do {
            BeatInfo currentBeat = 
                BeatInfo.fromPathStats((short) (in.getLineNumber()-2),
                                       currentLine,
                                       nextLine);
            result.beats.add(currentBeat);
            currentLine = nextLine;
            nextLine = in.readLine();
        } while (null != nextLine);

        result.computeMaximumOverdrive();
        result.computeReachableVocalAndDrumMeters();
        return result;
    }

    public void computeMaximumOverdrive() {
        short currentMaximumOverdrive[] = new short[Instrument.INSTRUMENT_COUNT.index()];
        double currentWhammy[] = new double[Instrument.INSTRUMENT_COUNT.index()];
        currentWhammy[Instrument.GUITAR.index()] = 
        currentWhammy[Instrument.BASS.index()] = 0.0;
        for (BeatInfo currentBeat : this.beats) {
            currentMaximumOverdrive = currentMaximumOverdrive.clone();

            for (int i = 0; i < 4; ++i) {
                currentMaximumOverdrive[i] +=
                    currentBeat.hasOverdrivePhraseEnd(i) ?  OVERDRIVE_PHRASE : 0;
            }

            if (currentBeat.hasUnisonBonusPhraseEnd()) {
                currentMaximumOverdrive[Instrument.GUITAR.index()] += OVERDRIVE_PHRASE;
                currentMaximumOverdrive[Instrument.BASS.index()] += OVERDRIVE_PHRASE;
                currentMaximumOverdrive[Instrument.DRUMS.index()] += OVERDRIVE_PHRASE;
            }

            updateWhammy(currentBeat, currentMaximumOverdrive, currentWhammy);
            Util.truncateOverdriveMeters(currentMaximumOverdrive);
            currentBeat.maximumOverdriveBar = currentMaximumOverdrive;
        }
    }

    private static void updateWhammy(BeatInfo currentBeat, short[] overdrive,
                                     double[] whammy) {
        whammy[Instrument.GUITAR.index()] += currentBeat.guitarWhammy;
        whammy[Instrument.BASS.index()] += currentBeat.bassWhammy;
        overdrive[Instrument.GUITAR.index()] += Math.floor(whammy[Instrument.GUITAR.index()]);
        overdrive[Instrument.BASS.index()] += Math.floor(whammy[Instrument.BASS.index()]);
        whammy[Instrument.GUITAR.index()] -= Math.floor(whammy[Instrument.GUITAR.index()]);
        whammy[Instrument.BASS.index()] -= Math.floor(whammy[Instrument.BASS.index()]);
    }

    public void computeReachableVocalAndDrumMeters() {
        this.computeReachableMeters(Instrument.VOCALS);
        this.computeReachableMeters(Instrument.DRUMS);
    }

    public void computeReachableMeters(Instrument instrument) {
        final int songLength = this.beats.size();
        for (BeatInfo currentBeat : this.beats) {
            // there are a few meters that are always reachable
            for (int i = 0; i*OVERDRIVE_PHRASE <= currentBeat.maximumOverdrive(instrument); ++i) {

                currentBeat.setReachableMeter(instrument, i * OVERDRIVE_PHRASE);
            }

            if (!currentBeat.instrumentCanActivate(instrument) ||
                (currentBeat.maximumOverdrive(instrument) < OVERDRIVE_HALFBAR)) {
                continue;
            }

            // okay, we have enough overdrive, and we can activate
            // walk through the vocal track. ,setting
            //      N, N-1*sbpb, N-2*sbpb, ...
            //      N-8*sbpb, N-9*sbpb, ...
            //      N-16*sbpb, N-17*sbpb, ...
            // ... to true. Where N = maximumOverdrive

            short initialOverdrive = currentBeat.maximumOverdrive(instrument);

            do {
                short overdriveRemaining = initialOverdrive;
                short currentBeatNumber = currentBeat.beatNumber;
                do { 
                    BeatInfo beatInOverdrive = this.beats.get(currentBeatNumber);
                    // we always want to assume the "squeeze in" for the sake of
                    // reachability. So we check for phrase endings before we
                    // check to see if we're done
                    beatInOverdrive.canBeInOverdrive[instrument.index()] = true;
                    beatInOverdrive.setReachableMeter(instrument, overdriveRemaining);
                    if (beatInOverdrive.hasOverdrivePhraseEnd(instrument)) {
                        overdriveRemaining += OVERDRIVE_PHRASE;
                    }

                    if (beatInOverdrive.hasUnisonBonusPhraseEnd() &&
                        !Instrument.VOCALS.equals(instrument)) {
                        overdriveRemaining += OVERDRIVE_PHRASE;
                    }


                    ++currentBeatNumber;
                    --overdriveRemaining;

                    overdriveRemaining = (short) Math.min(overdriveRemaining, OVERDRIVE_FULLBAR);
                } while ((overdriveRemaining > 0) && (currentBeatNumber < songLength));

                initialOverdrive -= OVERDRIVE_PHRASE;
            } while (initialOverdrive >= OVERDRIVE_HALFBAR);
        }
    }

    public static SongInfo fromMid2TxtOutput(String fileName) throws Exception {
        return null;
    }

    public void computeReachableStates(int beatNumber, Collection< BandState > results) throws Exception {
        // start with "no one active, everyone with current maximumOverdriveBar"
        // this state is always theortically reachable simply by never
        // activating
        BeatInfo currentBeat = this.beats.get(beatNumber);
        BandState firstBandState = new BandState();
        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            firstBandState.setInstrumentInOverdrive(i, false);
            firstBandState.setInstrumentMeter(i,currentBeat.maximumOverdriveBar[i]);
        }
        results.add(firstBandState);
        //
        // toggle activations. Now we may have as many as 16 states. Some of
        // these may be unreachable (e.g. near the end of verse three of
        // "Where'd You Go" vocals must be inactive), but that's okay; the final
        // forward walk will ignore these.

        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            ArrayList< BandState > tmp = new ArrayList< BandState >(16);
            for (BandState tmpState : results) {
                BandState newState = (BandState) (tmpState.clone());
                if ((currentBeat.maximumOverdriveBar[i] >= OVERDRIVE_HALFBAR) &&
                    currentBeat.canBeInOverdrive[i]) {
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
            for (short i = (short) (currentBeat.maximumOverdriveBar[Instrument.GUITAR.index()]-1); i >= 0; --i) {
                for (BandState tmpState : results) {
                    BandState newState = (BandState) (tmpState.clone());
                    newState.setInstrumentMeter(Instrument.GUITAR.index(), i);
                    tmp.add(newState);
                }
            }
            results.addAll(tmp);
        }

        {
            ArrayList< BandState >  tmp = new ArrayList< BandState >(results.size());
            for (short i = (short) (currentBeat.maximumOverdriveBar[Instrument.BASS.index()]-1); i >= 0; --i) {
                for (BandState tmpState : results) {
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
            for (short i = (short) (currentBeat.maximumOverdriveBar[Instrument.DRUMS.index()]-1); i >= 0; --i) {
                if (currentBeat.isReachableMeter(Instrument.DRUMS, i)) {
                    for (BandState tmpState : results) {
                        BandState newState = (BandState) (tmpState.clone());
                        newState.setInstrumentMeter(Instrument.DRUMS.index(), i);
                        tmp.add(newState);
                    }
                }
            }
            results.addAll(tmp);
        }

        {
            ArrayList< BandState >  tmp = new ArrayList< BandState >(results.size());
            for (short i = (short) (currentBeat.maximumOverdriveBar[Instrument.VOCALS.index()]-1); i >= 0; --i) {
                if (currentBeat.isReachableMeter(Instrument.VOCALS, i)) {
                    for (BandState tmpState : results) {
                        BandState newState = (BandState) (tmpState.clone());
                        newState.setInstrumentMeter(Instrument.VOCALS.index(), i);
                        tmp.add(newState);
                    }
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

    public void computeReachableStatesGivenCurrentBandStates(int beatNumber, BandState currentBandState, Collection< BandState > results) throws Exception {
        //
        BeatInfo currentBeat = this.beats.get(beatNumber);
        BandState initialBandState = (BandState) currentBandState.clone();
        boolean canSqueezeDrums = initialBandState.canSqueeze(Instrument.DRUMS, currentBeat);

        results.add(initialBandState);
        // apply whammy (does not affect activation)

        {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());
            for (BandState tmpState : results) {
                BandState newBandState = (BandState) tmpState.clone();
                newBandState.applyWhammy(Instrument.GUITAR, currentBeat);
            }

            results.addAll(tmp);
        }

        {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());
            for (BandState tmpState : results) {
                BandState newBandState = (BandState) tmpState.clone();
                newBandState.applyWhammy(Instrument.BASS, currentBeat);
            }

            results.addAll(tmp);
        }


        // advance all the instruments that are in overdrive (affects
        // activation)
        {
            ArrayList< BandState > tmp = new ArrayList< BandState >(results.size());

            for (BandState tmpState : results) {

                tmpState.advanceActivatedInstruments(currentBeat, false);
                if (canSqueezeDrums) {
                    BandState squeezedBandState = (BandState) tmpState.clone();
                    squeezedBandState.advanceActivatedInstruments(currentBeat, true);
                    tmp.add(squeezedBandState);
                }
            }

            results.addAll(tmp);
        }


        // toggle activations for any instrument that is in a position to do so
        ArrayList< BandState > preActivationStates = new ArrayList< BandState > (results.size());
        preActivationStates.addAll(results);

        for (BandState tmpState : preActivationStates) {
            for(int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
                if (currentBeat.instrumentCanActivate(i, tmpState)) {
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

        // assert(results.size() < 128
    }

    public static void main(String[] args) throws Exception{
        String fileName = args[0];
        System.out.println("extracting songInfo from \"" + fileName + "\"");
        SongInfo info = SongInfo.fromPathStatsFile(args[0]);
        for (BeatInfo beatInfo : info.beats()) {
            System.out.println(beatInfo);
        }

        ArrayList< BandState > bandStates = new ArrayList< BandState >();
        for (int i = 0; i < info.beats().size(); ++i) {
            bandStates.clear();
            info.computeReachableStates(i, bandStates);
            System.out.println("Beat " + i + " has " + bandStates.size() + " states");
        }
    }

}
