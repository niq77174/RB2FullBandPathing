package com.scorehero.pathing.fullband;
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
import java.io.File;
import java.io.Serializable;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class MultiLayeredScoredBeat extends ScoredBeat {
    private  Short2ObjectMap< Short2IntOpenHashMap > outerMap = 
        new Short2ObjectRBTreeMap< Short2IntOpenHashMap >();

    public MultiLayeredScoredBeat() {
        super();
    }

    private final static int SUFFIX_BITS = 0x0000ffff;
    private final static int PREFIX_BITS = 0xffff0000;

    private static short getOuterKey(BandState bandState) {
        return (short) ((bandState.serializedData() & PREFIX_BITS) >> 16);
    }

    private static short getInnerKey(BandState bandState) {
        return (short) (bandState.serializedData() & SUFFIX_BITS);
    }

    public int getScore(BandState bandState) {
        final short outerKey = getOuterKey(bandState);
        Short2IntOpenHashMap currentMap = this.outerMap.get(outerKey);
        if (null == currentMap) {
            // ruh-roh! This should never happen.
            throw new RuntimeException("Couldn't find map:\n" + bandState);
        }

        final short innerKey = getInnerKey(bandState);
        int scoredBandState = currentMap.get(innerKey);
        if (-1 == scoredBandState) {
            // ruh-roh! This should never happen.
            throw new RuntimeException("Couldn't find score:\n" + bandState);
        }
        
        return scoredBandState;
    }

    public void addScore(BandState bandState, int score) {
        final short outerKey = getOuterKey(bandState);

        Short2IntOpenHashMap currentMap = this.outerMap.get(outerKey);
        if (null == currentMap) {
            currentMap = new Short2IntOpenHashMap(1024);
            currentMap.defaultReturnValue(-1);
            this.outerMap.put(outerKey, currentMap);
        }

        final short innerKey = getInnerKey(bandState);
        final int oldScore = currentMap.get(innerKey);

        if ((score > oldScore) || (0 == score)) {
            currentMap.put(innerKey, score);
        }
    }

    public void flush(String title, int beatNumber) throws Exception {
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(getFileName(title, beatNumber)));
        // Writer stream = new BufferedWriter(new FileWriter(getFileName(title, beatNumber)));
        System.out.println("Dumping elements");
        System.out.println("Outer map size: " + this.outerMap.size());

        byte[] buf = new byte[8];


        try {
            // header tells us how many elements
            // not needed; can be inferred from file size
            //Util.toByteArray(this.outerMap.size(), buf);
            //stream.write(buf);
            BandState tmp = new BandState();

            ObjectSet< Short2ObjectMap.Entry< Short2IntOpenHashMap > > entrySet = this.outerMap.short2ObjectEntrySet();
            int elementsWritten = 0;
            for (Short2ObjectMap.Entry< Short2IntOpenHashMap > outerEntry : entrySet) {
                int highBits = outerEntry.getShortKey() << 16;
                Short2IntOpenHashMap currentMap = outerEntry.getValue();
                elementsWritten += currentMap.size();

                ObjectIterator< Short2IntMap.Entry > i = currentMap.short2IntEntrySet().fastIterator();
                while (i.hasNext()) {
                    Short2IntMap.Entry entry = i.next();
                    final int fullKey = highBits | (entry.getShortKey() & 0x0000ffff);
                    final int score = entry.getIntValue();
                    Util.toByteArray(fullKey, 0, buf);
                    Util.toByteArray(score, 4, buf);
                    stream.write(buf);
                }
            }
            System.out.println("Total elements written: " + elementsWritten);

        } finally {
            stream.close();
        }
    }

    public void close() {

    }

    public static ScoredBeat fromDisk(String title, int beatNumber) throws IOException {
        final String fileName = getFileName(title, beatNumber);
        File theFile = new File(fileName);
        if (!theFile.exists()) {
            throw new IOException("can't find " + theFile);
        }

        long numElements = theFile.length() / 8L;

        MultiLayeredScoredBeat result = new MultiLayeredScoredBeat();
        InputStream stream = new BufferedInputStream(new FileInputStream(theFile));
        BandState currentState = new BandState();
        byte[] buf = new byte[8];
        for (long i = 0; i < numElements; ++i) {
            stream.read(buf);
            currentState.setBits(Util.toInteger(buf, 0));
            final int score = Util.toInteger(buf, 4);
            result.addScore(currentState, score);
        }
        return result;
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

}
