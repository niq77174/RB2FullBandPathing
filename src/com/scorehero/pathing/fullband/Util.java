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
        Util.toByteArray(n, 0, result);
    }
    
    public static void toByteArray(int n, int offset, byte[] result) {
        result[offset+0] = (byte) ((n >>> 24) & 0xff);
        result[offset+1] = (byte) ((n >>> 16) & 0xff);
        result[offset+2] = (byte) ((n >>> 8) & 0xff);
        result[offset+3] = (byte) (n & 0xff);
    }

    public static int toInteger(byte[] byteArray) {
        return Util.toInteger(byteArray, 0);
    }

    public static int toInteger(byte[] byteArray, int offset) {
        int result = 0;
        result |= (byteArray[offset] << 24) & 0xff000000;
        result |= (byteArray[offset+1] << 16) & 0x00ff0000;
        result |= (byteArray[offset+2] << 8) & 0x0000ff00;
        result |= byteArray[offset+3] & 0x000000ff;
        return result;
    }

    public static void main(String[] args) {
        int blah = 0xBAADFEED;
        int foo = 0x04900EEF;
        byte[] arr = new byte[8];
        Util.toByteArray(foo, 0, arr);
        Util.toByteArray(blah, 4, arr);
        int test = Util.toInteger(arr, 4);
        System.out.println(Integer.toHexString(blah));
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < arr.length; ++i) {
            hex.append(Integer.toHexString(0xff & arr[i]));
            hex.append(" ");
        }
        System.out.println(hex);
        System.out.println(Integer.toHexString(test));
        assert(test == blah);
    }
}
