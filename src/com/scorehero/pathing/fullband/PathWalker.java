package com.scorehero.pathing.fullband;
import java.util.Collection;
import java.util.ArrayList;

public class PathWalker {
    private static boolean debugOutput = false;
    public PathWalker() {
    }

    public void findPath(SongInfo songInfo, 
                         Collection< BandState > result) throws Exception {
        // read back the first scored beat from disk
        ScoredBeat firstBeat = MultiLayeredScoredBeat.fromDisk(songInfo.title(), 0);
        // set current state to default state
        BandState currentState = (BandState) BandState.INITIAL_BANDSTATE.clone();
        result.add(currentState);
        int score = firstBeat.getScore(currentState);

        for (int i = 0; i < songInfo.beats().size()-1; ++i) {
            ArrayList< BandState > nextStates = new ArrayList< BandState >();
            BeatInfo currentBeat = songInfo.beats().get(i);
            // compute all reachable next states for current state S
            currentBeat.computeReachableNextStates(currentState, nextStates);

            ScoredBeat nextBeat = MultiLayeredScoredBeat.fromDisk(songInfo.title(), i+1);
            for (BandState nextState : nextStates) {

                // play through the beat
                // compute score K for current beat with current state pair
                int beatScore = currentBeat.score(currentState, nextState);
                // find any (all?) next states in next beat with Score S-K
                if (nextBeat.getScore(nextState) == (score - beatScore)) {
                    // close out the currentState
                    currentState = nextState;
                    score -= beatScore;
                    break;
                }
            }

            result.add(currentState);
        }

        // set state to their states
        // advance
    }

    private final static String[] INSTRUMENT_NAME = {
        "guitar",
        "drums",
        "vocals",
        "bass"
    };

    public static void main(String[] args) throws Exception {
        PathWalker.debugOutput = true;

        SongInfo song = SongInfo.fromMid2TxtFile(args[0]);
        boolean verbose = args.length > 1 && "--verbose".equals(args[1]);
        ArrayList< BandState > path = new ArrayList< BandState >(song.beats().size());
        PathWalker pathWalker = new PathWalker();
        pathWalker.findPath(song, path);

        if (verbose) {
            System.out.print("Beat 0");
        }
        BandState currentState = path.get(0);
        int score = 0;
        for (int i = 0; i < path.size()-1; ++i) {
            BandState nextState = path.get(i+1);
            BeatInfo currentBeat = song.beats().get(i);
            int beatScore = currentBeat.score(currentState, nextState);
            score += beatScore;
            StringBuilder output = new StringBuilder();
            currentState = nextState;
            if (verbose) {
                output.append(" Score: " + score + " Beat Score: " + beatScore);
                output.append("\nBeat " + (i+1) + " " + nextState);
                System.out.print(output);
            }
        }

        int beatScore = song.beats().get(path.size()-1).score(currentState, BandState.INITIAL_BANDSTATE);
        score += beatScore;
        if (verbose) {
            System.out.println(" Score: " + score + " Beat Score: " + beatScore);
        }

        System.out.println("user niq24601 PASSWORD");
        System.out.println("option ppqn 30");
        System.out.println("color textblack 0 0 0");
        System.out.println("color odyellow 255 240 192");
        
        int startTicks[] = new int[Instrument.INSTRUMENT_COUNT.index()];
        ArrayList< BeatInfo > beats = song.beats();
        int currentMeasure = 0;
        score = 0;
        int measureScore = 0;
        currentState = path.get(0);

        //StringBuilder drumPath = new StringBuilder("string -30 40 Drum Path: ")
        for (int i = 0; i < path.size()-1; ++i) {
            BandState nextState = path.get(i+1);
            BeatInfo currentBeat = beats.get(i);

            if (currentBeat.isDownBeat()) {
                if (currentMeasure > 0) {
                    System.out.println("measurescore drums " + currentMeasure + " " + measureScore);
                    System.out.println("totalscore drums " + currentMeasure + " " + score);
                }
                measureScore = 0;
                ++currentMeasure;
            }
            beatScore = currentBeat.score(currentState, nextState);
            measureScore += beatScore;
            score += beatScore;


            for (int j = 0; j < Instrument.INSTRUMENT_COUNT.index(); ++j) {
                if (!currentState.instrumentInOverdrive(j) && nextState.instrumentInOverdrive(j)) {
                    startTicks[j] = currentBeat.startTicks();
                }

                if (currentState.instrumentInOverdrive(j) && !nextState.instrumentInOverdrive(j)) {
                    final int endTicks = currentBeat.endTicks();
                    StringBuilder currentLine = new StringBuilder("fill ");
                    currentLine.append(INSTRUMENT_NAME[j]);
                    currentLine.append(" odyellow ");
                    currentLine.append(startTicks[j]);
                    currentLine.append(" ");
                    currentLine.append(endTicks);
                    System.out.println(currentLine);
                }
            }

            currentState = nextState;
        }

        /*
        int songLen = path.size()-1;
        int score = 0;
        for (int i = 0; i < songLen ; ++i) {
            BeatInfo currentBeat = song.getBeat(i);
            BandState currentState = path.get(i+1);
            BandState nextState = path.get(i+1);
            score += beatScore;
            if (currentState.instrumentInOverdrive(Instrument.GUITAR) ||
                nextState.instrumentInOverdrive(Instrument.GUITAR)) {
            }

            if (currentState.instrumentInOverdrive(Instrument.DRUMS) ||
                nextState.instrumentInOverdrive(Instrument.DRUMS)) {
            }

            if (currentState.instrumentInOverdrive(Instrument.VOCALS) ||
                nextState.instrumentInOverdrive(Instrument.VOCALS)) {
            }

            if (currentState.instrumentInOverdrive(Instrument.BASS) ||
                nextState.instrumentInOverdrive(Instrument.BASS)) {
            }
        }
        */
    }
}
