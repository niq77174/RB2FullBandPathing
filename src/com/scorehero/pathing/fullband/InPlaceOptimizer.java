package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;

public class InPlaceOptimizer {
    private static boolean debugOutput = false;
    public InPlaceOptimizer() {
    }

    public void optimize(SongInfo songInfo, 
                         Collection< BandState > result) throws Exception {
        ScoredBeat nextBeat = new StubScoredBeat();
        for (int i = 0; i < songInfo.beats().size(); ++i) {
            int beatNumber = songInfo.beats().size()-1-i;
            BeatInfo currentBeat = songInfo.beats().get(beatNumber);
            ScoredBeat currentScoredBeat = this.optimizeBeat(currentBeat, nextBeat);
            // We're done with the next beat
            nextBeat.flush(songInfo.title(), beatNumber+1);
            nextBeat.close();
            // and walk backwards a step
            nextBeat = currentScoredBeat;
        }
        nextBeat.flush(songInfo.title(), 0);

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

            nextBeat = ScoredBeat.fromFile(songInfo.title(), i+1);
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

    private final static int MAX_BAND_STATES = 256;

    private ScoredBeat optimizeBeat(BeatInfo beatInfo, ScoredBeat nextBeat) throws Exception {
        ScoredBeat result = new StandardScoredBeat();
        System.out.println("Optimizing " + beatInfo);
        final int reachableStateCount = beatInfo.computeReachableStateCount();
        System.out.println("state count estimate: " + beatInfo);
        ArrayList< BandState > bandStates =  new ArrayList< BandState >();
        //beatInfo.computeReachableStates(bandStates);
        int totalNextStateCount = 0;
        ArrayList< BandState > nextBeatStates = new ArrayList< BandState >(MAX_BAND_STATES);

        for (int i = 0; i < MAX_BAND_STATES; ++i) {
            nextBeatStates.add(new BandState());
        }

        BandState bandState = (BandState) BandState.INITIAL_BANDSTATE.clone();
        BandState nextState = (BandState) BandState.INITIAL_BANDSTATE.clone();
        boolean hasNextReachableMeter = true;

        int beatStateCount = 0;

        while (hasNextReachableMeter) {
            int nextStateCount = beatInfo.computeReachableNextStatesInPlace(bandState, nextBeatStates);
            totalNextStateCount += nextStateCount;

            int score = 0;
            for (int i = 0; i < nextStateCount; ++i) {
                BandState nextBeatState = nextBeatStates.get(i);
                int beatScore = beatInfo.score(bandState, nextBeatState);
                score = Math.max(score, beatScore + nextBeat.getScore(nextBeatState));
            }

            result.addScore(bandState, score);

            ++beatStateCount;
            hasNextReachableMeter = beatInfo.computeNextReachableState(bandState, nextState);
            nextState.copyTo(bandState);
        }

        System.out.println("Actual beat state count: " + beatStateCount);
        System.out.println("Scores calculated: " + totalNextStateCount);
        System.out.println("Scores/state: " + ((double) totalNextStateCount)/((double) bandStates.size())); 
        return result;
    }


    public static void main(String[] args) throws Exception {
        InPlaceOptimizer.debugOutput = true;

        ArrayList< BandState > path = new ArrayList< BandState >();
        SongInfo song = SongInfo.fromMid2TxtFile(args[0]);
        /*
        for (BeatInfo beatInfo : song.beats()) {
            System.out.println(beatInfo + "\n");
        }
        */

        InPlaceOptimizer optimizer = new InPlaceOptimizer();
        optimizer.optimize(song, path);

        System.out.println("beat 0");
        BandState currentState = path.get(0);
        int score = 0;
        for (int i = 0; i < path.size()-1; ++i) {
            BandState nextState = path.get(i+1);
            BeatInfo currentBeat = song.beats().get(i);
            int beatScore = currentBeat.score(currentState, nextState);
            score += beatScore;
            System.out.println("Score: " + score + " Beat Score: " + beatScore);
            System.out.println("beat " + (i+1));
            currentState = nextState;
            System.out.println(nextState);
        }

        int beatScore = song.beats().get(path.size()-1).score(currentState, BandState.INITIAL_BANDSTATE);
        score += beatScore;
        System.out.println("Score: " + score + " Beat Score: " + beatScore);
    }
}
