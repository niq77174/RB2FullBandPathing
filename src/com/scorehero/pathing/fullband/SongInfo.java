package com.scorehero.pathing.fullband;

import java.util.ArrayList;
import java.util.Collection;
import java.io.LineNumberReader;
import java.io.FileReader;

public class SongInfo {
    private ArrayList< BeatInfo > beats;

    public final static int SUBBEATS_PER_BEAT = 2 ; // will be larger for some songs
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
                BeatInfo.fromPathStats((short) in.getLineNumber(),
                                       currentLine,
                                       nextLine);
            result.beats.add(currentBeat);
            currentLine = nextLine;
            nextLine = in.readLine();
        } while (null != nextLine);

        result.calculateMaximumOverdrive();
        result.calculateReachableOverdriveAmounts();
        return result;
    }

    public void calculateMaximumOverdrive() {
        short currentMaximumOverdrive[] = new short[Instrument.INSTRUMENT_COUNT.index()];
        double currentGuitarWhammy = 0.0, currentBassWhammy = 0.0;
        for (BeatInfo currentBeat : this.beats) {
            for (int i = 0; i < 4; ++i) {
                currentBeat.maximumOverDriveBar[i] = currentMaximumOverdrive[i];
                currentMaximumOverdrive[i] +=
                    currentBeat.hasLastBeatOfInstrumentOverDrive[i] ?  OVERDRIVE_PHRASE : 0;
                currentMaximumOverdrive[i] += currentBeat.hasLastBeatOfUnisonBonus ? OVERDRIVE_PHRASE: 0;
            }

            currentGuitarWhammy += 1.1 * currentBeat.guitarWhammy;
            currentMaximumOverdrive[Instrument.GUITAR.index()] += Math.floor(currentGuitarWhammy);
            currentGuitarWhammy -= Math.floor(currentGuitarWhammy);

            currentBassWhammy += 1.1 * currentBeat.bassWhammy;
            currentMaximumOverdrive[Instrument.BASS.index()] += Math.floor(currentGuitarWhammy);
            currentBassWhammy -= Math.floor(currentBassWhammy);

            Util.truncateOverdriveMeters(currentMaximumOverdrive);
            for (int i = 0; i < 4; ++i) {
                currentMaximumOverdrive[i] =
                    (short) Math.min(currentMaximumOverdrive[i], OVERDRIVE_FULLBAR);
            }
        }
    }

    public void calculateReachableOverdriveAmounts() {
        // walk through the vocal track. If the beat has an activation point and
        // max OD is above 16*SUBBEATS_PER_BEAT, walk forward setting 
        // N, N-8*sbpb, N-16*sbpb
        // N-1, N-9*sbpb, N-17*sbpb
        // ... to true. If you encounter another OD phrase

        /*
        for (BeatInfo currentBeat : beats) {
            if ((currentBeat.maximumOverdriveBar[Instrument.VOCALS.index()] > 16 *
                SongInfo.SUBBEATS_PER_BEAT) &&
                (currentBeat.instrumentCanActivate[Instrument.VOCALS.index()]) {

            }
        }
        */

        // Same thing for drums
    }

    public static SongInfo fromMid2TxtOutput(String fileName) throws Exception {
        return null;
    }

    public void computeReachableStates(int beatNumber, Collection< BandState > bandStates) {
        // start with "no one active, everyone with current maximumOverDriveBar"
        // this state is always theortically reachable simply by never
        // activating
        //
        // toggle activations. Now up to 16 states. Some of these may be
        // unreachable (e.g. in verse three of "Where'd You Go" vocals must be
        // inactive)

        // first the easy part: iterate over all possible amounts of overdrive
        // for guitar and bass in [0,maximumOverdriveBar)
        //
        // technically, this will compute some unreachable states, (e.g. guitar
        // with 23 beats if that amount of whammy is unattainable at certain
        // times in the song. Guitar with 14 beats 1 beat after first possible
        // activation, etc.), but that's okay ... the forward walk will prevent
        // us from encountering those values (right? right?)

        // clone each state, tweaking the guitar overdrive
        //
        // same for bass
        //

        // now possibly up to 16*65^2 = 67600 states

        // now iterate through each of the reachable vocals states with the same
        // clone/tweak combination

        // now iterate through each of the reachable drum states with the same
        // clone/tweak combination

        // unclear on how many states we are talking about here. For drums the
        // typical measure will have about 9 reachable states (off-beat
        // activations will make this number larger). For vocals, verses and
        // chorus are usually unable to activate more than half the time. But
        // sometimes there are long sections with no singing or taps. Assume it
        // averages out to 33 vocal states, we're probably near 20M states
        // per beat

    }

    public static void main(String[] args) throws Exception{
        String fileName = args[0];
        System.out.println("extracting songInfo from \"" + fileName + "\"");
        SongInfo info = SongInfo.fromPathStatsFile(args[0]);
        for (BeatInfo beatInfo : info.beats()) {
            System.out.println(beatInfo);
        }
    }

}
