package com.scorehero.pathing.fullband;

public class Util {
    private static byte truncateOverdriveMeter(byte overdriveMeter) {
        return (byte) Math.min(overdriveMeter, SongInfo.OVERDRIVE_FULLBAR);
    }

    public static void truncateOverdriveMeters(byte overdriveMeter[]) {
        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            overdriveMeter[i] = Util.truncateOverdriveMeter(overdriveMeter[i]);
        }
    }

    public static void truncateOverdriveMeters(BandState bandState) {
        for (int i = 0; i < Instrument.INSTRUMENT_COUNT.index(); ++i) {
            final byte newMeter = Util.truncateOverdriveMeter(bandState.getInstrumentMeter(i));
            bandState.setInstrumentMeter(i, newMeter);
        }
    }

    public static byte[] toByteArray(int n) {
        byte[] result = new byte[4];
        Util.toByteArray(n, result);
        return result;
    }

    public static void toByteArray(int n, byte[] result) {
        result[0] = (byte) (n & 0xff);
        result[1] = (byte) ((n >>> 8) & 0xff);
        result[2] = (byte) ((n >>> 16) & 0xff);
        result[3] = (byte) ((n >>> 24) & 0xff);
    }

    public static int toInteger(byte[] byteArray) {
        int result = 0;
        result |= byteArray[0];
        result |= (byteArray[1] << 8);
        result |= (byteArray[2] << 16);
        result |= (byteArray[3] << 24);

        return result;
    }
}
