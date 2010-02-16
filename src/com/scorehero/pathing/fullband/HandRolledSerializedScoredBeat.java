package com.scorehero.pathing.fullband;
import java.util.TreeMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import java.io.Serializable;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class HandRolledSerializedScoredBeat extends StandardScoredBeat implements Serializable{

    public HandRolledSerializedScoredBeat() {
        super();
        this.scoredBandStateMap = 
            new TreeMap< Integer, Integer >( );
    }

    public void flush(String title, int beatNumber) throws Exception {
        System.out.println("flushing " + this.scoredBandStateMap.size() +  " elements");
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(getFileName(title, beatNumber)));
        System.out.println("Dumping " + this.scoredBandStateMap.size() +  " elements");
        byte[] buf = new byte[4];


        try {
            // header tells us how many elements
            Util.toByteArray(this.scoredBandStateMap.size(), buf);
            stream.write(buf);

            Set< Map.Entry< Integer, Integer > > entrySet = this.scoredBandStateMap.entrySet();
            for (Map.Entry< Integer, Integer > entry : entrySet) {
                Util.toByteArray(entry.getKey().intValue(), buf);
                stream.write(buf);
                Util.toByteArray(entry.getValue().intValue(), buf);
                stream.write(buf);
            }

        } finally {
            stream.close();
        }
    }

    public static ScoredBeat fromDisk(String title, int beatNumber) {
        // IntBuffer buffer = fileChannel.map().asIntBuffer();
        /*
        final int fileSize = fileChannel.size();
        final int numInts = fileSize / 4;
        return new IntBufferScoredBeat(intBuffer, numInts);
        */
        return null;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    }

    private final static String STORAGE_ROOT = "/home/niq/.big/handrolled";
    private final static String FILE_PREFIX = "beat_";

    private final static String getFileName(String title, int beatNumber) {
        StringBuilder result = new StringBuilder(STORAGE_ROOT);
        result.append("/");
        result.append(title);
        result.append("/");
        result.append(FILE_PREFIX);

        if (beatNumber < 1000) {
            result.append('0');
        }

        if (beatNumber < 100) {
            result.append('0');
        }

        if (beatNumber < 10) {
            result.append('0');
        }

        result.append(beatNumber);
        //System.out.println("Searching for " + result);
        return result.toString();
    }


    public void close() {
    }

}
