package com.scorehero.pathing.fullband;

import java.util.ArrayList;
import java.util.Collection;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.Random;

public class SongInfo {
    private ArrayList< BeatInfo > beats;
    private String title;

    public final static int SUBBEATS_PER_BEAT = 1 ; // will be larger for some songs
    public final static int OVERDRIVE_PHRASE = 8 * SUBBEATS_PER_BEAT; 
    public final static int OVERDRIVE_HALFBAR = 2 * OVERDRIVE_PHRASE;
    public final static int OVERDRIVE_FULLBAR = 4 * OVERDRIVE_PHRASE;

    // not always song-level data :/
    byte beatsPerMeasure;
    byte lengthOfBeat;

    public String title() {
        return this.title;
    }

    public ArrayList< BeatInfo > beats() {
        return this.beats;
    }

    public static SongInfo fromPathStatsFile(String title,
                                             String fileName) throws Exception {
        SongInfo result = SongInfo.fromPathStatsFile(fileName);
        result.title = title;
        return result;
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
        byte currentMaximumOverdrive[] = new byte[Instrument.INSTRUMENT_COUNT.index()];
        for (int j = 1; j < this.beats.size(); ++j) {
            BeatInfo currentBeat = this.beats.get(j);
            BeatInfo previousBeat = this.beats.get(j-1);

            currentMaximumOverdrive = currentMaximumOverdrive.clone();

            for (int i = 0; i < 4; ++i) {
                currentMaximumOverdrive[i] +=
                    previousBeat.hasOverdrivePhraseEnd(i) ?  OVERDRIVE_PHRASE : 0;
            }

            if (previousBeat.hasUnisonBonusPhraseEnd()) {
                currentMaximumOverdrive[Instrument.GUITAR.index()] += OVERDRIVE_PHRASE;
                currentMaximumOverdrive[Instrument.BASS.index()] += OVERDRIVE_PHRASE;
                currentMaximumOverdrive[Instrument.DRUMS.index()] += OVERDRIVE_PHRASE;
            }

            // fix whammy stuff

            Util.truncateOverdriveMeters(currentMaximumOverdrive);
            currentBeat.maximumOverdriveBar = currentMaximumOverdrive;
        }
    }


    public void computeReachableVocalAndDrumMeters() {
        this.computeReachableMeters(Instrument.VOCALS);
        this.computeReachableMeters(Instrument.DRUMS);
    }

    public void computeReachableMeters(Instrument instrument) {
        final int songLength = this.beats.size();
        for (BeatInfo currentBeat : this.beats) {
            // there are a few meters that are always reachable
            /*
            for (int i = 0; i*OVERDRIVE_PHRASE <= currentBeat.maximumOverdrive(instrument); ++i) {

                currentBeat.setReachableMeter(instrument, i * OVERDRIVE_PHRASE);
            }
            */

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

    public static void main(String[] args) throws Exception {
        String fileName = args[0];
        System.out.println("extracting songInfo from \"" + fileName + "\"");
        SongInfo info = SongInfo.fromPathStatsFile(args[0]);
        ArrayList< BandState > bandStates = new ArrayList< BandState >();

        for (BeatInfo beatInfo : info.beats()) {
            System.out.println(beatInfo + "\n");
        }

        /*
        for (int i = 0; i < info.beats().size(); ++i){
            info.computeReachableStates(i, bandStates);
            System.out.println("Beat " + i + " has " + bandStates.size() + " states");
            bandStates.clear();
        }
        Random rng = new Random();

        int totalNextStates = 0;
        for(int i = 0; i < 20; ++i) {
            int currentBeatNum = rng.nextInt(info.beats().size());
            BeatInfo currentBeat = info.beats().get(currentBeatNum);
            System.out.println("next beat");
            System.out.println(currentBeat);
            currentBeat.computeReachableStates(bandStates);
            ArrayList< BandState > nextStates = new ArrayList< BandState > ();
            for (int j = 0; j < 20; ++j) {
                BandState theState = bandStates.get(rng.nextInt(bandStates.size()));
                System.out.println("current bandstates");
                System.out.println(theState);
                currentBeat.computeReachableNextStates(theState, nextStates);
                System.out.println("next bandstates");
                totalNextStates += nextStates.size();
                for (BandState bandState : nextStates) {
                    System.out.println(bandState);
                }
                System.out.println("====");
                nextStates.clear();
            }
            bandStates.clear();
        }
        System.out.println("total next states: " + totalNextStates);
        */
    }

}
