package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;

public class Optimizer {
    private static boolean debugOutput = false;
    public Optimizer() {
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

    private ScoredBeat optimizeBeat(BeatInfo beatInfo, ScoredBeat nextBeat) throws Exception {
        ScoredBeat result = new StandardScoredBeat();
        ArrayList< BandState > bandStates =  new ArrayList< BandState>();
        beatInfo.computeReachableStates(bandStates);
        System.out.println("Optimizing " + beatInfo);
        System.out.println("Current beat state count: " + bandStates.size());
        int nextBeatStateCount = 0;
        for (BandState bandState : bandStates) {
            ArrayList< BandState > nextStates = new ArrayList< BandState >();
            beatInfo.computeReachableNextStates(bandState, nextStates);
            nextBeatStateCount += nextStates.size();

            int score = 0;
            for (BandState nextState: nextStates) {
                int beatScore = beatInfo.score(bandState, nextState);
                score = Math.max(score, beatScore + nextBeat.getScore(nextState));
            }

            result.addScore(bandState, score);
        }
        System.out.println("Scores calculated: " + nextBeatStateCount);
        System.out.println("Scores/state: " + ((double) nextBeatStateCount)/((double) bandStates.size())); 
        return result;
    }

    public static void main(String[] args) throws Exception {
        Optimizer.debugOutput = true;

        ArrayList< BandState > path = new ArrayList< BandState >();
        SongInfo song = SongInfo.fromMid2TxtFile(args[0]);
        for (BeatInfo beatInfo : song.beats()) {
            System.out.println(beatInfo + "\n");
        }

        Optimizer optimizer = new Optimizer();
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
