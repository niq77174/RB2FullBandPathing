package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class DIYStorageOptimizer {
    private static boolean debugOutput = false;

    private final static int BAND_STATES_SIZES[] = {
                0,
          2097152,
         16777216,
         50331648,
        100663296
    };

    private IntArrayList bandStates;

    public DIYStorageOptimizer() {
        this(SongInfo.SUBBEATS_PER_BEAT);
    }

    public DIYStorageOptimizer(int subBeatsPerBeat) {
        this.bandStates = new IntArrayList(BAND_STATES_SIZES[subBeatsPerBeat]);
    }

    public void optimize(SongInfo songInfo, 
                         Collection< BandState > result) throws Exception {
        ScoredBeat nextBeat = new StubScoredBeat();
        long maxBeatTime = 0;
        final long startTime = System.currentTimeMillis();
        long lastBeatEndTime = startTime;
        for (int i = 0; i < songInfo.beats().size(); ++i) {
            int beatNumber = songInfo.beats().size()-1-i;
            BeatInfo currentBeat = songInfo.beats().get(beatNumber);
            ScoredBeat currentScoredBeat = this.optimizeBeat(currentBeat, nextBeat);
            // We're done with the next beat
            nextBeat.flush(songInfo.title(), beatNumber+1);
            nextBeat.close();
            // and walk backwards a step
            nextBeat = currentScoredBeat;
            final long beatEndTime = System.currentTimeMillis();
            final long thisBeatTime = beatEndTime - lastBeatEndTime;
            maxBeatTime = Math.max(maxBeatTime, thisBeatTime);
            lastBeatEndTime = beatEndTime;

            double averageBeatTime =  0.001*((double) beatEndTime - startTime)/((double) (i+1));
            System.out.println("Average beat time: " + averageBeatTime);
            System.out.println("Peak beat time: " + (maxBeatTime / 1000) + "." + (maxBeatTime % 1000));
            final long totalTime = beatEndTime - startTime;
            System.out.println("Total time: " + (totalTime / 1000) + "." + (totalTime % 1000));
        }
        nextBeat.flush(songInfo.title(), 0);
        final long beatEndTime = System.currentTimeMillis();
        final long thisBeatTime = beatEndTime - lastBeatEndTime;
        maxBeatTime = Math.max(maxBeatTime, thisBeatTime);

        double averageBeatTime =  0.001*((double) beatEndTime - startTime)/((double) songInfo.beats().size());
        System.out.println("Average beat time: " + averageBeatTime);
        System.out.println("Peak beat time: " + (maxBeatTime / 1000) + "." + (maxBeatTime % 1000));
        final long totalTime = beatEndTime - startTime;
        System.out.println("Total time: " + (totalTime / 1000) + "." + (totalTime % 1000));


        // read back the first scored beat from disk
        /*
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
        */

        // set state to their states
        // advance
    }

    private final static int MAX_BAND_STATES = 256;

    private ScoredBeat optimizeBeat(BeatInfo beatInfo, ScoredBeat nextBeat) throws Exception {
        ScoredBeat result = new MultiLayeredScoredBeat();
        System.out.println("Optimizing " + beatInfo);
        /*

        final int reachableStateCount = beatInfo.computeReachableStateCount();
        System.out.println("state count estimate: " + reachableStateCount);
        ArrayList< BandState > bandStates =  new ArrayList< BandState >();
        beatInfo.computeReachableStates(bandStates);
        int totalNextStateCount = 0;
        ArrayList< BandState > nextBeatStates = new ArrayList< BandState >(MAX_BAND_STATES);

        for (int i = 0; i < MAX_BAND_STATES; ++i) {
            nextBeatStates.add(new BandState());
        }

        BandState bandState = (BandState) BandState.INITIAL_BANDSTATE.clone();
        BandState nextState = (BandState) BandState.INITIAL_BANDSTATE.clone();
        boolean hasNextReachableState = true;

        int beatStateCount = 0;

        while (hasNextReachableState) {
            int nextStateCount = beatInfo.computeReachableNextStatesInPlace(bandState, nextBeatStates);
            totalNextStateCount += nextStateCount;

            int score = 0;
            for (int i = 0; i < nextStateCount; ++i) {
                BandState nextBeatState = nextBeatStates.get(i);
                int beatScore = beatInfo.score(bandState, nextBeatState);
                score = Math.max(score, beatScore + nextBeat.getScore(bandState, nextBeatState));
            }

            result.addScore(bandState, score);


            ++beatStateCount;
            hasNextReachableState = beatInfo.computeNextReachableState(bandState, nextState);
            nextState.copyTo(bandState);
        }

        System.out.println("Actual beat state count: " + beatStateCount);
        System.out.println("Scores calculated: " + totalNextStateCount);
        System.out.println("Scores/state: " + ((double) totalNextStateCount)/((double) bandStates.size())); 
        return result;
        */

        final int reachableStateCount = beatInfo.computeReachableStateCount();
        System.out.println("state count estimate: " + reachableStateCount);
        //int paddedStateCount = (int) (1.3 * reachableStateCount);
        //this.bandStates.ensureCapacity(paddedStateCount);
        this.bandStates.clear();
        int actualStateCount = beatInfo.computeReachableStates(this.bandStates);
        System.out.println("Actual beat state count: " + actualStateCount);

        int totalNextStateCount = 0;
        ArrayList< BandState > nextStates = new ArrayList< BandState >(MAX_BAND_STATES);

        for (int i = 0; i < MAX_BAND_STATES; ++i) {
            nextStates.add(new BandState());
        }

        BandState bandState = new BandState();
        for (int j = 0; j < actualStateCount; ++j) {
            bandState.setBits(this.bandStates.get(j));
            int nextStateCount = beatInfo.computeReachableNextStatesInPlace(bandState, nextStates);
            totalNextStateCount += nextStateCount;

            int score = 0;
            for (int i = 0; i < nextStateCount; ++i) {
                BandState nextState = nextStates.get(i);
                int beatScore = beatInfo.score(bandState, nextState);
                score = Math.max(score, beatScore + nextBeat.getScore(nextState));
            }

            result.addScore(bandState, score);
        }

        System.out.println("Scores calculated: " + totalNextStateCount);
        System.out.println("Scores/state: " + ((double) totalNextStateCount)/((double) actualStateCount)); 
        return result;
    }


    public static void main(String[] args) throws Exception {
        DIYStorageOptimizer.debugOutput = true;

        ArrayList< BandState > path = new ArrayList< BandState >();
        SongInfo song = SongInfo.fromMid2TxtFile(args[0]);
        /*
        for (BeatInfo beatInfo : song.beats()) {
            System.out.println(beatInfo + "\n");
        }
        */

        DIYStorageOptimizer optimizer = new DIYStorageOptimizer();
        optimizer.optimize(song, path);

        /*
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
        */
    }
}
