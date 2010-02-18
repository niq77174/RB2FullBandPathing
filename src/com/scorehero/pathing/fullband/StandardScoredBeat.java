package com.scorehero.pathing.fullband;
import java.util.TreeMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Comparator;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;

public class StandardScoredBeat extends ScoredBeat {
    protected Int2IntSortedMap scoredBandStateMap;
    private final static Comparator< Integer > INTEGER_COMPARATOR = new Comparator< Integer >() {
        public int compare(Integer left, Integer right) {
            return left.compareTo(right);
        }
    };

    public StandardScoredBeat() {
        super();
        this.scoredBandStateMap = new Int2IntRBTreeMap();
        this.scoredBandStateMap.defaultReturnValue(-1);
    }

    public void addScore(BandState bandState, int score) {
        int key = bandState.serializedData();
        int oldScore = this.scoredBandStateMap.get(key);

        if ((score > oldScore) || (0 == score)) {
            this.scoredBandStateMap.put(key, score);
        }
    }

    public int getScore(BandState bandState) {

        int scoredBandState = scoredBandStateMap.get(bandState.serializedData());
        
        return scoredBandState;
    }

    public void flush(String title, int beatNumber) throws Exception {
        Database database = BDBScoredBeat.getDB(title, beatNumber, false);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        Set< Map.Entry< Integer, Integer > > entrySet = this.scoredBandStateMap.entrySet();
        System.out.println("done building entrySet");
        for (Map.Entry< Integer, Integer > entry : entrySet) {
            IntegerBinding.intToEntry(entry.getKey().intValue(), key);
            IntegerBinding.intToEntry(entry.getValue().intValue(), value);
            database.put(null, key, value);
        }
        Environment env = database.getEnvironment();
        /*
        StatsConfig config = new StatsConfig();
        config.setClear(true);
        System.out.println(env.getStats(config));
        */
        database.close();
        env.close();
    }

    public void close() {
    }

}
