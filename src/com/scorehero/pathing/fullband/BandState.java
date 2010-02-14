package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;

public class BandState implements Cloneable {
    private byte instrumentMeter[];
    private boolean instrumentInOverdrive[];
    public final static BandState INITIAL_BANDSTATE = buildInitialBandState();

    private static BandState buildInitialBandState() {
        BandState result = new BandState();
        result.instrumentInOverdrive[0] = result.instrumentInOverdrive[1] = result.instrumentInOverdrive[2] = result.instrumentInOverdrive[3] = false;
        result.instrumentMeter[0] = result.instrumentMeter[1] = result.instrumentMeter[2] = result.instrumentMeter[3];
        return result;
    }

    public BandState() {
        this.instrumentInOverdrive = new boolean[Instrument.INSTRUMENT_COUNT.index()];
        this.instrumentMeter = new byte[Instrument.INSTRUMENT_COUNT.index()];
    }

    public int serializedData() {
        int result = 0;
        for (int i = 0; i < instrumentMeter.length; ++i) {
            result <<= 7;
            result |= instrumentMeter[i] & 0x7f;
        }
        for (int i = 0; i < instrumentInOverdrive.length; ++i) {
            result <<= 1;
            result |= instrumentInOverdrive[i] ? 1 : 0;
        }

        return result;
    }

    public static BandState fromSerializedData(int value) {
        return null; // TODO
    }

    public void applyWhammy(Instrument instrument, BeatInfo beatInfo) {
        this.instrumentMeter[instrument.index()] += beatInfo.getWhammy(instrument);
    }

    public boolean canSqueeze(Instrument instrument, BeatInfo beatInfo) {
        return (1 == instrumentMeter[instrument.index()] && beatInfo.hasSqueezeAvailable(instrument));
    }


    public void advanceActivatedInstruments(BeatInfo beatInfo, boolean squeezeDrums) {
        for (int i = 0; i < this.instrumentMeter.length; ++i) {
            if (this.instrumentInOverdrive(i)) {
                --this.instrumentMeter[i];
                if (0 == this.instrumentMeter[i]) {
                    this.instrumentInOverdrive[i] = false;
                }
            }
        }

        Util.truncateOverdriveMeters(this.instrumentMeter);
    }

    public void activateInstrument(int instrument) {
        this.instrumentInOverdrive[instrument] = true;
    }

    public void acquireUnisonBonus() {
        this.acquireOverdrive(Instrument.GUITAR);
        this.acquireOverdrive(Instrument.BASS);
        this.acquireOverdrive(Instrument.DRUMS);
    }

    public void drainOverdrive(Instrument instrument) {
        --this.instrumentMeter[instrument.index()];
        //assert(this.instrumentMeter[instrumentMeter.index()] > 0);
    }

    public void acquireOverdrive(Instrument instrument) {
        this.acquireOverdrive(instrument.index());
    }

    public void acquireOverdrive(int instrument) {
        this.instrumentMeter[instrument] += SongInfo.OVERDRIVE_PHRASE;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        /*
        result.append("BandState: ");
        String binaryString = Integer.toBinaryString(this.serializedData());
        for (int i = 0; i < (32 - binaryString.length()); ++i) {
            result.append("0");
        }
        result.append(binaryString);
        */
        result.append("Meter: ");
        result.append(instrumentMeter[Instrument.GUITAR.index()]);
        result.append("|" + instrumentMeter[Instrument.DRUMS.index()]);
        result.append("|" + instrumentMeter[Instrument.VOCALS.index()]);
        result.append("|" + instrumentMeter[Instrument.BASS.index()]);
        result.append("  Overdrive: ");
        result.append((instrumentInOverdrive[Instrument.GUITAR.index()] ? 'Y' : 'N'));
        result.append("|" + (instrumentInOverdrive[Instrument.DRUMS.index()] ? 'Y' : 'N'));
        result.append("|" + (instrumentInOverdrive[Instrument.VOCALS.index()] ? 'Y' : 'N'));
        result.append("|" + (instrumentInOverdrive[Instrument.BASS.index()] ? 'Y' : 'N'));
        return result.toString();
    }

    public void setInstrumentInOverdrive(int instrument, boolean value) {
        this.instrumentInOverdrive[instrument] = value;
    }

    public void setInstrumentMeter(int instrument, byte value) {
        this.instrumentMeter[instrument] = value;
    }

    public short getInstrumentMeter(int instrument) {
        return this.instrumentMeter[instrument];
    }

    public short getInstrumentMeter(Instrument instrument) {
        return this.getInstrumentMeter(instrument.index());
    }

    public boolean instrumentInOverdrive(int instrument) {
        return this.instrumentInOverdrive[instrument];
    }

    public boolean instrumentInOverdrive(Instrument instrument) {
        return this.instrumentInOverdrive(instrument.index());
    }

    public boolean instrumentCanActivate(Instrument instrument) {
        return (!this.instrumentInOverdrive(instrument) && this.getInstrumentMeter(instrument) >= SongInfo.OVERDRIVE_HALFBAR);
    }

    public Object clone() throws CloneNotSupportedException {
        BandState result = (BandState) super.clone();
        result.instrumentInOverdrive = this.instrumentInOverdrive.clone();
        result.instrumentMeter = this.instrumentMeter.clone();
        return result;
    }

}
