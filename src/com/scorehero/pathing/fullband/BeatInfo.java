package com.scorehero.pathing.fullband;

import java.util.StringTokenizer;
import java.util.Arrays;

public class BeatInfo implements Cloneable {
    short beatNumber;
    short measureNumber;
    double beatWithinMeasure; // between 0.0 and 1.0
    short score;
    short drumScore; // 0 if not covered by fill
    double guitarWhammy; // between 0.0 and 1.0;
    double bassWhammy; // between 0.0 and 1.0;
    boolean hasLastBeatOfInstrumentOverdrive[];
    boolean hasLastBeatOfUnisonBonus;
    boolean instrumentCanActivate[];
    short maximumOverdriveBar[];

    boolean canBeInOverdrive[];
    boolean isReachableMeter[][];

    public BeatInfo() {
        this.hasLastBeatOfInstrumentOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentCanActivate = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentCanActivate[Instrument.GUITAR.index()] =
        this.instrumentCanActivate[Instrument.BASS.index()] = true;
        this.canBeInOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.canBeInOverdrive[Instrument.GUITAR.index()] = true;
        this.canBeInOverdrive[Instrument.BASS.index()] = true;

        this.maximumOverdriveBar = new short[Instrument.INSTRUMENT_COUNT.index()];

        this.isReachableMeter = new boolean[Instrument.INSTRUMENT_COUNT.index()][SongInfo.OVERDRIVE_FULLBAR+1];
    }

    public short maximumOverdrive(Instrument instrument) {
        return this.maximumOverdriveBar[instrument.index()];
    }

    public boolean instrumentCanActivate(Instrument instrument) {
        return this.instrumentCanActivate(instrument.index());
    }

    public boolean instrumentCanActivate(int instrumentIndex) {
        return (this.instrumentCanActivate[instrumentIndex]);
    }

    public boolean instrumentCanActivate(Instrument instrument,
                                         BandState bandState) {
        return this.instrumentCanActivate(instrument.index(), bandState);
    }

    public boolean instrumentCanActivate(int instrumentIndex,
                                         BandState bandState) {
        return ((this.instrumentCanActivate[instrumentIndex]) && 
                (bandState.getInstrumentMeter(instrumentIndex) > SongInfo.OVERDRIVE_HALFBAR));
    }

    public void setReachableMeter(Instrument instrument,
                                  int meter) {
        this.isReachableMeter[instrument.index()][meter] = true;
    }

    public boolean isReachableMeter(Instrument instrument,
                                    int meter) {
        return this.isReachableMeter[instrument.index()][meter];
    }

    public short getBeatScoreForBandState(BandState bandState) {
        short result = this.score;

        // subtract out the drum score if this is part of an overdrive fill
        if (this.instrumentCanActivate(Instrument.DRUMS.index(), bandState) &&
            !bandState.instrumentInOverdrive(Instrument.DRUMS.index())) {
            result -= this.drumScore;
        }

        int instrumentsInOverdrive = 0;
        for (int i = 0; i < 4; ++i) {
            instrumentsInOverdrive += bandState.instrumentInOverdrive(i) ? 1 : 0;
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
        result.instrumentCanActivate[Instrument.VOCALS.index()] = currentBeat.contains("vox tacet");

        result.hasLastBeatOfInstrumentOverdrive[Instrument.BASS.index()] = nextBeat.contains("bass od");
        result.hasLastBeatOfInstrumentOverdrive[Instrument.VOCALS.index()] = nextBeat.contains("vox od");
        result.hasLastBeatOfInstrumentOverdrive[Instrument.DRUMS.index()] = nextBeat.contains("drum od");
        result.hasLastBeatOfInstrumentOverdrive[Instrument.GUITAR.index()] = nextBeat.contains("guitar od");

        if (nextBeat.contains("unison bonus")) {
            result.hasLastBeatOfInstrumentOverdrive[Instrument.GUITAR.index()] = 
                result.hasLastBeatOfInstrumentOverdrive[Instrument.BASS.index()] =
                result.hasLastBeatOfInstrumentOverdrive[Instrument.DRUMS.index()] = 
                result.hasLastBeatOfUnisonBonus = true;
        }
        
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Overall Beat Number: ");
        result.append(this.beatNumber);
        result.append("  Measure/Beat number: ");
        result.append(((double) this.measureNumber) + this.beatWithinMeasure);
        result.append("\n Score:");
        result.append(this.score);
        result.append(", Drum Score: ");
        result.append(this.drumScore);
        result.append("\n Overdrive Phrase: Guitar:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.GUITAR.index()]);
        result.append(", Drums:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.DRUMS.index()]);
        result.append(", Vocals:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.VOCALS.index()]);
        result.append(", Bass:");
        result.append(this.hasLastBeatOfInstrumentOverdrive[Instrument.BASS.index()]);
        result.append("\n Maximum Overdrive: Guitar:");
        result.append(this.maximumOverdriveBar[Instrument.GUITAR.index()]);
        result.append(", Drums:");
        result.append(this.maximumOverdriveBar[Instrument.DRUMS.index()]);
        result.append(", Vocals:");
        result.append(this.maximumOverdriveBar[Instrument.VOCALS.index()]);
        result.append(", Bass:");
        result.append(this.maximumOverdriveBar[Instrument.BASS.index()]);
        result.append("\n");

        result.append("\n Activations: Drums: ");
        result.append(this.instrumentCanActivate[Instrument.DRUMS.index()] ? 'Y' : 'N');
        result.append(" Vocals: ");
        result.append(this.instrumentCanActivate[Instrument.VOCALS.index()] ? 'Y' : 'N');

        result.append(" Reachable Meters:\n  Drums:");
        for (int i = 0; i < this.isReachableMeter[Instrument.DRUMS.index()].length; ++i) {
            result.append(this.isReachableMeter[Instrument.DRUMS.index()][i] ?  "Y," : "N,");
        }

        result.append("\n  Vocal:");
        for (int i = 0; i < this.isReachableMeter[Instrument.VOCALS.index()].length; ++i) {
            result.append(this.isReachableMeter[Instrument.VOCALS.index()][i] ?  "Y," : "N,");
        }


        // whammy? activation locations?

        return result.toString();
    }

    public boolean hasOverdrivePhraseEnd(int instrument) {
        return this.hasLastBeatOfInstrumentOverdrive[instrument];
    }

    public boolean hasOverdrivePhraseEnd(Instrument instrument) {
        return this.hasOverdrivePhraseEnd(instrument.index());
    }

    public boolean hasUnisonBonusPhraseEnd() {
        return this.hasLastBeatOfUnisonBonus;
    }

}
