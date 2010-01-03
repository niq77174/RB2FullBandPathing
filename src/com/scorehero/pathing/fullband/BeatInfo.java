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
        this.isReachableDrumState = new boolean[SongInfo.OVERDRIVE_FULLBAR];
        this.isReachableVocalState = new boolean[SongInfo.OVERDRIVE_FULLBAR];
    }

    public boolean instrumentCanActivate(int instrumentIndex,
                                         BandState bandState) {
        return ((this.instrumentCanActivate[instrumentIndex]) && 
                (bandState.instrumentMeter[instrumentIndex] > SongInfo.OVERDRIVE_HALFBAR));
    }

    public short getBeatScoreForBandState(BandState bandState) {
        short result = this.score;

        if (this.instrumentCanActivate(Instrument.DRUMS.index(), bandState) &&
            !bandState.instrumentInOverdrive[Instrument.DRUMS.index()]) {
            result -= this.drumScore;
        }

        int instrumentsInOverdrive = 0;
        for (int i = 0; i < 4; ++i) {
            instrumentsInOverdrive += bandState.instrumentInOverdrive[i]? 1 : 0;
        }

        // this is devious.
        // The first tirm here is straight forward. Just 2x # instruments
        // The second term is a 0-1 variable that is 0 if and only if
        // instruments == 0. So with nothing in overdrive, it reduces to
        //      0 + 1 = 1
        // But for all other cases it reduces to
        //      2 * N + 0 == 2* N

        // assert(instruments <=4, "more than 4 instruments in overdrive");
        short multiplier = (short)
            (2*instrumentsInOverdrive + (1-(instrumentsInOverdrive+2)/3));

        return (short) (result * multiplier);
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

        String dummy1 = tok.nextToken(); // skip the beat count
        String dummy2 = tok.nextToken(); // skip the OD notes
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
