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
import java.io.IOException;

public class SerializedScoredBeat extends StandardScoredBeat implements Serializable {

    public SerializedScoredBeat() {
        super();
    }



    public void flush(String title, int beatNumber) {
        System.out.println("Serializing " + this.scoredBandStateMap.size() +  " elements");
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

    public static void fromDisk(String title, int beatNumber) {
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    }


    public void close() {
    }

}
