package com.scorehero.pathing.fullband;

public class Util {
    private static short truncateOverdriveMeter(short overdriveMeter) {
        return (short) Math.min(overdriveMeter, SongInfo.OVERDRIVE_FULLBAR);
    }

    public static void truncateOverdriveMeters(short overdriveMeter[]) {
        for (int i = 0; i < overdriveMeter.length; ++i) {
            overdriveMeter[i] = Util.truncateOverdriveMeter(overdriveMeter[i]);
        }
    }
}
