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
        ScoredBeat firstBeat = ScoredBeat.fromFile(songInfo.title(), 0);
        // set current state to default state
        BandState currentState = (BandState) BandState.INITIAL_BANDSTATE.clone();
        result.add(currentState);
        int score = firstBeat.getScore(currentState);

        for (int i = 0; i < songInfo.beats().size()-1; ++i) {
            ArrayList< BandState > nextStates = new ArrayList< BandState >();
            BeatInfo currentBeat = songInfo.beats().get(i);
            // compute all reachable next states for current state S
            currentBeat.computeReachableNextStates(currentState, nextStates);

            ScoredBeat nextBeat = ScoredBeat.fromFile(songInfo.title(), i+1);
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

    public static void main(String[] args) throws Exception {
        PathWalker.debugOutput = true;

        ArrayList< BandState > path = new ArrayList< BandState >();
        SongInfo song = SongInfo.fromMid2TxtFile(args[0]);
        PathWalker pathWalker = new PathWalker();
        pathWalker.findPath(song, path);

        System.out.print("Beat 0");
        BandState currentState = path.get(0);
        int score = 0;
        for (int i = 0; i < path.size()-1; ++i) {
            BandState nextState = path.get(i+1);
            BeatInfo currentBeat = song.beats().get(i);
            int beatScore = currentBeat.score(currentState, nextState);
            score += beatScore;
            StringBuilder output = new StringBuilder();
            currentState = nextState;
            output.append(" Score: " + score + " Beat Score: " + beatScore);
            output.append("\nBeat " + (i+1) + " " + nextState);
            System.out.print(output);
        }

        int beatScore = song.beats().get(path.size()-1).score(currentState, BandState.INITIAL_BANDSTATE);
        score += beatScore;
        System.out.println("Score: " + score + " Beat Score: " + beatScore);

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
