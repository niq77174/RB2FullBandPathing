package com.scorehero.pathing.fullband;

import java.util.Collection;
import java.util.ArrayList;

public class BandState implements Cloneable {
    //private byte instrumentMeter[];
    //private boolean instrumentInOverdrive[];
    private int bitSet;

    private final static int METER_OFFSETS[] = {
        21, 0, 7, 14
    };

    private final static int OD_BITS_OFFSET = 28;

    private final static int OD_BITS[] = {
        0x80000000,
        0x40000000,
        0x20000000,
        0x10000000
    };

    private final static int ALL_OD_BITS = 0xf0000000;


    private final static int METER_BITS[] = {
        0x7f <<  METER_OFFSETS[0],
        0x7f <<  METER_OFFSETS[1],
        0x7f <<  METER_OFFSETS[2],
        0x7f <<  METER_OFFSETS[3]
    };

    private final static int INV_METER_BITS[] = {
        ~METER_BITS[0],
        ~METER_BITS[1],
        ~METER_BITS[2],
        ~METER_BITS[3]
    };

    public final static BandState INITIAL_BANDSTATE = buildInitialBandState();

    private static BandState buildInitialBandState() {
        BandState result = BandState.fromSerializedData(0);
        return result;
    }

    public BandState() {
    }

    public void copyTo(BandState dst) {
        dst.bitSet = this.bitSet;
    }

    public int serializedData() {
        return bitSet;
    }

    public static BandState fromSerializedData(int value) {
        BandState result = new BandState();
        result.bitSet = value;
        return result;
    }

    public void applyWhammy(Instrument instrument, BeatInfo beatInfo) {
        this.setInstrumentMeter(instrument, (byte) (this.getInstrumentMeter(instrument) + beatInfo.getWhammy(instrument)));
    }

    public boolean canSqueeze(Instrument instrument, BeatInfo beatInfo) {
        return (1 == this.getInstrumentMeter(instrument) && beatInfo.hasSqueezeAvailable(instrument));
    }


    public void advanceActivatedInstruments(BeatInfo beatInfo, boolean squeezeDrums) {
        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            if (this.instrumentInOverdrive(i)) {
                byte newMeter = (byte) (this.getInstrumentMeter(i) - 1);
                newMeter = (byte) (newMeter >= 0 ? newMeter : 0);
                this.setInstrumentMeter(i, newMeter);
                if (0 == newMeter) {
                    if (i != Instrument.DRUMS.index() || !squeezeDrums) {
                        this.setInstrumentInOverdrive(i, false);
                    }
                }
            }
        }

        Util.truncateOverdriveMeters(this);
    }

    public void activateInstrument(int instrument) {
        this.bitSet |= OD_BITS[instrument];
    }

    public void acquireUnisonBonus() {
        this.acquireOverdrive(Instrument.GUITAR);
        this.acquireOverdrive(Instrument.BASS);
        this.acquireOverdrive(Instrument.DRUMS);
    }

    public void drainOverdrive(Instrument instrument) {
        this.setInstrumentMeter(instrument, (byte) (this.getInstrumentMeter(instrument)-1));
        //assert(this.instrumentMeter[instrumentMeter.index()] > 0);
    }

    public void acquireOverdrive(Instrument instrument) {
        this.acquireOverdrive(instrument.index());
    }

    public void acquireOverdrive(int instrument) {
        this.setInstrumentMeter(instrument, (byte) (this.getInstrumentMeter(instrument) + SongInfo.OVERDRIVE_PHRASE));
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("BandState: ");
        String binaryString = Integer.toBinaryString(this.bitSet);
        for (int i = 0; i < (32 - binaryString.length()); ++i) {
            result.append("0");
        }
        result.append(binaryString);
        result.append(" Meter: ");
        result.append(this.getInstrumentMeter(Instrument.GUITAR));
        result.append("|" + this.getInstrumentMeter(Instrument.DRUMS));
        result.append("|" + this.getInstrumentMeter(Instrument.VOCALS));
        result.append("|" + this.getInstrumentMeter(Instrument.BASS));
        result.append("  Overdrive: ");
        result.append(this.instrumentInOverdrive(Instrument.GUITAR) ? 'Y' : 'N');
        result.append("|" + (this.instrumentInOverdrive(Instrument.DRUMS) ? 'Y' : 'N'));
        result.append("|" + (this.instrumentInOverdrive(Instrument.VOCALS) ? 'Y' : 'N'));
        result.append("|" + (this.instrumentInOverdrive(Instrument.BASS) ? 'Y' : 'N'));
        return result.toString();
    }

    private final static String toFullBinaryString(int n) {
        String binaryString = Integer.toBinaryString(n);
        StringBuilder result = new StringBuilder();
        final int len = binaryString.length();
        for (int i = 0; i < (32 - len); ++i) {
            result.append("0");
        }
        result.append(binaryString);
        return result.toString();
    }

    public void setInstrumentInOverdrive(int instrument, boolean value) {
        if (value) {
            this.bitSet |= OD_BITS[instrument];
        } else {
            this.bitSet &= ~OD_BITS[instrument];
        }
    }
    public void setInstrumentMeter(Instrument instrument, byte value) {
        this.setInstrumentMeter(instrument.index(), value);
    }

    public void setInstrumentMeter(int instrument, byte value) {
        //assert(value <= SongInfo.OVERDRIVE_FULLBAR);
        this.bitSet &= INV_METER_BITS[instrument];
        this.bitSet |= (value << METER_OFFSETS[instrument]);
    }

    public byte getInstrumentMeter(int instrument) {
        /*
        System.out.println("instrument: " + instrument);
        System.out.println("bitset: " + toFullBinaryString(this.bitSet));
        final int mask = METER_BITS[instrument];
        System.out.println("maskw/: " + toFullBinaryString(mask));
        int bits = (this.bitSet & mask);
        System.out.println("masked: " + toFullBinaryString(bits));
        */
        //byte result = (byte) (bits >>> METER_OFFSETS[instrument]) ;
        byte result = (byte) ((this.bitSet & METER_BITS[instrument]) >>> METER_OFFSETS[instrument]) ;
        return result;
    }

    public byte getInstrumentMeter(Instrument instrument) {
        return this.getInstrumentMeter(instrument.index());
    }

    public boolean instrumentInOverdrive(int instrument) {
        return ((this.bitSet & OD_BITS[instrument]) != 0);
    }

    public boolean instrumentInOverdrive(Instrument instrument) {
        return this.instrumentInOverdrive(instrument.index());
    }

    public boolean instrumentCanActivate(Instrument instrument) {
        return (!this.instrumentInOverdrive(instrument) && this.getInstrumentMeter(instrument) >= SongInfo.OVERDRIVE_HALFBAR);
    }

    public byte odBits() {
        return (byte) ((this.bitSet & ALL_OD_BITS) >> OD_BITS_OFFSET);
    }

    public void setODBits(byte odBits) {
        odBits &= ALL_OD_BITS;
        final int oldHighBits = this.bitSet & ~ALL_OD_BITS;
        this.bitSet &= ~ALL_OD_BITS;
        this.bitSet |= odBits;
    }

    public void setBits(int bits) {
        this.bitSet = bits;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
