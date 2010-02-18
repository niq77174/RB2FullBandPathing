package com.scorehero.pathing.fullband;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.Random;
import java.util.StringTokenizer;

public class SongInfo {
        public final static int SUBBEATS_PER_BEAT = 1; // will be larger for some songs
        public final static int OVERDRIVE_PHRASE = 8 * SUBBEATS_PER_BEAT; 
        public final static int OVERDRIVE_HALFBAR = 2 * OVERDRIVE_PHRASE;
        public final static int OVERDRIVE_FULLBAR = 4 * OVERDRIVE_PHRASE;
        private final static HashMap< String, TrackHandler > trackHandlers = buildTrackHandlerHash();

        private ArrayList< BeatInfo > beats;
        private String title;
        private byte beatsPerMeasure; // not always song-level data
        private byte lengthOfBeat;
        private TreeMap< Integer, BeatInfo > nearestBeatMap;

        public static boolean DEBUG_OUTPUT;

    public SongInfo() {
        this.nearestBeatMap = new TreeMap< Integer, BeatInfo >();
    }
    
    public void addBeat(int startTicks, int endTicks) {
        if (SongInfo.DEBUG_OUTPUT) {
            System.out.println("Adding [" + startTicks + ", " + endTicks + ")");
        }
        BeatInfo newBeat = new BeatInfo(startTicks, endTicks);
        this.beats.add(newBeat);
        newBeat.setBeatNumber((short) (this.beats.size()-1));
        this.nearestBeatMap.put(new Integer(startTicks), newBeat);
    }

    public String title() {
        return this.title;
    }

    public ArrayList< BeatInfo > beats() {
        return this.beats;
    }

    public BeatInfo getBeat(int beatNumber) {
        return this.beats.get(beatNumber);
    }

    public BeatInfo getNearestBeat(int tickCount) {
        Integer ticks = new Integer(tickCount);
        SortedMap< Integer, BeatInfo > headMap = 
            this.nearestBeatMap.headMap(ticks);
        SortedMap< Integer, BeatInfo > tailMap = 
            this.nearestBeatMap.tailMap(ticks);
        if (tailMap.firstKey().intValue() == tickCount) {
            return tailMap.get(tailMap.firstKey());
        }
        return headMap.get(headMap.lastKey());
    }

    private static HashMap< String, TrackHandler > buildTrackHandlerHash() {
        TrackHandler nopHandler = new NopTrackHandler();
        HashMap< String, TrackHandler > result =
            new HashMap< String, TrackHandler >();
        result.put("BEAT", new BeatTrackHandler());
        //result.put("DRUMS", nopHandler);
        result.put("DRUMS", new DrumTrackHandler(Instrument.DRUMS, 4));
        //result.put("BASS", nopHandler);
        result.put("BASS", new GuitarTrackHandler(Instrument.BASS, 6));
        //result.put("GUITAR", nopHandler);
        result.put("GUITAR", new GuitarTrackHandler(Instrument.GUITAR, 4));
        //result.put("VOCALS", nopHandler);
        result.put("VOCALS", new VocalTrackHandler(Instrument.VOCALS, 4));
        result.put("VENUE", nopHandler);
        result.put("EVENTS", nopHandler);
        result.put("TEMPO", new TempoTrackHandler());

        return result;
    }

    public static SongInfo fromPathStatsFile(String title,
                                             String fileName) throws Exception {
        SongInfo result = SongInfo.fromPathStatsFile(fileName);
        result.title = title;
        return result;
    }

    public static SongInfo fromMid2TxtFile(String fileName) throws Exception {
        SongInfo result = new SongInfo();
        result.beats = new ArrayList< BeatInfo >();
        LineNumberReader in = new LineNumberReader(new FileReader(fileName));
        String theLine = in.readLine();
        int ticksPerBeat = 0;
        do {
            StringTokenizer tok = new StringTokenizer(theLine, " \"");
            String firstTok = tok.nextToken();
            if ("MTrk".equals(firstTok) || "TrkEnd".equals(firstTok)) {
                theLine = in.readLine();
                continue;
            }

            if ("MFile".equals(firstTok)) {
                tok.nextToken();
                tok.nextToken();
                ticksPerBeat = Integer.valueOf(tok.nextToken()).intValue();
                theLine = in.readLine();
                continue;
            }

            // okay, we're near a track start
            tok.nextToken();
            tok.nextToken();
            String track = tok.nextToken();
            
            //System.out.println("track is: " + track);
            if (SongInfo.trackHandlers.containsKey(track)) {
                TrackHandler trackHandler = SongInfo.trackHandlers.get(track);
                theLine = trackHandler.handleTrack(in, result);
            } else {
                if (tok.hasMoreTokens()) {
                    String instrument = tok.nextToken();
                    if (SongInfo.DEBUG_OUTPUT) {
                        System.out.println("instrument is: " + instrument);
                    }
                    TrackHandler trackHandler = trackHandlers.get(instrument);
                    theLine = trackHandler.handleTrack(in, result);
                } else {

                    // must be the meta track
                    result.title = track;
                    theLine = trackHandlers.get("TEMPO").handleTrack(in,result);
                }
            }
        } while (null != theLine);

        result.computeUnisonBonus();
        result.computeMaximumOverdrive();
        result.computeReachableVocalAndDrumMeters();
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

    public void computeUnisonBonus() {
        BeatInfo lastBeatWithOverdrivePhraseEnd = null;

        for (BeatInfo currentBeat : this.beats) {
            if (currentBeat.hasLastOverdriveNote(Instrument.GUITAR) ||
                currentBeat.hasLastOverdriveNote(Instrument.BASS) ||
                currentBeat.hasLastOverdriveNote(Instrument.DRUMS)) {
                lastBeatWithOverdrivePhraseEnd = currentBeat;
            }

            if (currentBeat.hasOverdrivePhraseEnd(Instrument.GUITAR) &&
                currentBeat.hasOverdrivePhraseEnd(Instrument.BASS) &&
                currentBeat.hasOverdrivePhraseEnd(Instrument.DRUMS)) {
                lastBeatWithOverdrivePhraseEnd.setLastBeatOfUnisonBonus(true);
            }
        }
    }

    public void computeMaximumOverdrive() {
        byte currentMaximumOverdrive[] = new byte[Instrument.INSTRUMENT_COUNT.index()];
        for (int j = 1; j < this.beats.size(); ++j) {
            BeatInfo currentBeat = this.beats.get(j);
            BeatInfo previousBeat = this.beats.get(j-1);

            currentMaximumOverdrive = currentMaximumOverdrive.clone();

            for (int i = 0; i < 4; ++i) {
                currentMaximumOverdrive[i] +=
                    previousBeat.hasLastOverdriveNote(i) ?  OVERDRIVE_PHRASE : 0;
                currentMaximumOverdrive[i] += previousBeat.getWhammy(i);
            }

            if (previousBeat.hasLastBeatOfUnisonBonus()) {
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
                    if (beatInOverdrive.hasLastOverdriveNote(instrument) &&
                        ((beatInOverdrive != currentBeat) ||
                          beatInOverdrive.isVocalPhraseEnd())) {
                        overdriveRemaining += OVERDRIVE_PHRASE;
                    }

                    if (beatInOverdrive.hasLastBeatOfUnisonBonus() &&
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


    public static void main(String[] args) throws Exception {
        String fileName = args[0];
        System.out.println("extracting songInfo from \"" + fileName + "\"");
        if (args.length > 1) {
            System.out.println("ckecking verbose");
            if ("--verbose".equals(args[1])) {
                SongInfo.DEBUG_OUTPUT = true;
            }
        }
        SongInfo info = SongInfo.fromMid2TxtFile(fileName);
        ArrayList< BandState > bandStates = new ArrayList< BandState >();

        for (BeatInfo beatInfo : info.beats()) {
            System.out.println(beatInfo + "\n");
        }

        /*
        for (int i = 0; i < info.beats().size(); ++i){
            int stateCount = 0;
            BandState currentState = new BandState();
            BandState nextState = new BandState();
            BeatInfo currentBeat = info.getBeat(i);
            System.out.println(currentBeat + "\n");
            final int reachableStateCount = currentBeat.computeReachableStateCount();
            System.out.println("state count estimate: " + reachableStateCount);
            //currentState.setBits(BeatInfo.DEBUG_STATE);
            currentState.setBits(Integer.MIN_VALUE);
            if (currentBeat.isValidState(currentState)) {
                System.out.println(currentState);
                ++stateCount;
            }
            while (currentBeat.computeNextReachableState(currentState, nextState)) {
                ++stateCount;
                if (0 == stateCount % 6) {
                }
                nextState.copyTo(currentState);
            }
            System.out.println("Beat " + i + " has " + stateCount + " states");
        }
        */

        Random rng = new Random();

        int totalNextStates = 0;
        for(int i = 0; i < 20; ++i) {
            //int currentBeatNum = rng.nextInt(info.beats().size());
            int currentBeatNum = 88;
            BeatInfo currentBeat = info.beats().get(currentBeatNum);
            System.out.println("next beat");
            System.out.println(currentBeat);
            currentBeat.computeReachableStates(bandStates);
            //ArrayList< BandState > nextStates = new ArrayList< BandState > ();
            for (BandState theState : bandStates) {
                ArrayList< BandState > nextStates = new ArrayList< BandState >(256);

                for (int k = 0; k < 256; ++k) {
                    nextStates.add(new BandState());
                }

                //BandState theState = bandStates.get(0);
                //BandState theState = bandStates.get(rng.nextInt(bandStates.size()));
                System.out.println("current bandstates");
                System.out.println(theState);
                int count = currentBeat.computeReachableNextStatesInPlace(theState, nextStates);
                System.out.println("next bandstates");
                for (int k = 0; k < count; ++k) {
                    System.out.println(nextStates.get(k));
                }
                System.out.println("====");
                nextStates.clear();
            }
            bandStates.clear();
        }
    }

}
