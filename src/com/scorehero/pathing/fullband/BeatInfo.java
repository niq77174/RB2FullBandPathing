package com.scorehero.pathing.fullband;

import java.util.StringTokenizer;

public class BeatInfo {
    short beatNumber;
    short measureNumber;
    double beatWithinMeasure; // between 0.0 and 1.0
    short score;
    short drumScore; // 0 if not covered by fill
    double guitarWhammy; // between 0.0 and 1.0;
    double bassWhammy; // between 0.0 and 1.0;
    boolean hasLastBeatOfInstrumentOverDrive[];
    boolean hasLastBeatOfUnisonBonus;
    boolean instrumentCanActivate[];
    short maximumOverDriveBar[];

    boolean isReachableVocalState[];
    boolean isReachableDrumState[];

    public BeatInfo() {
        this.hasLastBeatOfInstrumentOverDrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentCanActivate = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentCanActivate[Instrument.GUITAR.index()] =
        this.instrumentCanActivate[Instrument.BASS.index()] = true;

        this.maximumOverDriveBar = new short[Instrument.INSTRUMENT_COUNT.index()];
        this.isReachableDrumState = new boolean[32 * SongInfo.SUBBEATS_PER_BEAT + 1];
        this.isReachableVocalState = new boolean[32 * SongInfo.SUBBEATS_PER_BEAT + 1];
    }

    public static BeatInfo fromPathStats(short beatNumber,
                                         String currentBeat,
                                         String nextBeat) {
        BeatInfo result = new BeatInfo();
        result.beatNumber = beatNumber;

        StringTokenizer tok = new StringTokenizer (currentBeat, "\t");
        double parsedBeatNumber = Double.valueOf(tok.nextToken()).doubleValue();
        result.measureNumber = (short) parsedBeatNumber;
        result.beatWithinMeasure = parsedBeatNumber - ((double) result.measureNumber);

        tok.nextToken(); // skip the beat count
        tok.nextToken(); // skip the OD notes
        result.score = Short.valueOf(tok.nextToken()).shortValue();
        result.drumScore = Short.valueOf(tok.nextToken()).shortValue();

        result.instrumentCanActivate[Instrument.DRUMS.index()] = currentBeat.contains("drum act");

        result.hasLastBeatOfInstrumentOverDrive[Instrument.BASS.index()] = nextBeat.contains("bass od");
        result.hasLastBeatOfInstrumentOverDrive[Instrument.VOCALS.index()] = nextBeat.contains("vox od");
        result.hasLastBeatOfInstrumentOverDrive[Instrument.DRUMS.index()] = nextBeat.contains("drum od");
        result.hasLastBeatOfInstrumentOverDrive[Instrument.GUITAR.index()] = nextBeat.contains("guitar od");

        if (nextBeat.contains("unison bonus")) {
            result.hasLastBeatOfInstrumentOverDrive[Instrument.GUITAR.index()] = 
                result.hasLastBeatOfInstrumentOverDrive[Instrument.BASS.index()] =
                result.hasLastBeatOfInstrumentOverDrive[Instrument.DRUMS.index()] = 
                result.hasLastBeatOfUnisonBonus = true;
        }
        
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("OVERALL BEAT NUMBER: ");
        result.append(this.beatNumber);
        result.append("  MEASURE/SUBBEAT NUMBER: ");
        result.append(((double) this.measureNumber) + this.beatWithinMeasure);
        result.append("\n");
        result.append("SCORE: ");
        result.append(this.score);
        result.append(", DRUM SCORE: ");
        result.append(this.drumScore);
        result.append("\nOVERDRIVE: guitar:");
        result.append(this.hasLastBeatOfInstrumentOverDrive[Instrument.GUITAR.index()]);
        result.append(", drums:");
        result.append(this.hasLastBeatOfInstrumentOverDrive[Instrument.DRUMS.index()]);
        result.append(", vocals:");
        result.append(this.hasLastBeatOfInstrumentOverDrive[Instrument.VOCALS.index()]);
        result.append(", bass:");
        result.append(this.hasLastBeatOfInstrumentOverDrive[Instrument.BASS.index()]);
        result.append("\n");

        // whammy? activation locations?

        return result.toString();
    }
}
