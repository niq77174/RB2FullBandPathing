package com.scorehero.pathing.fullband;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class BDBScoredBeat extends ScoredBeat {
    private Database database;
    private static HashMap< String, Environment > dbEnvs = 
        new HashMap< String, Environment >();
    private static EnvironmentConfig roEnvConfig = buildROEnvConfig();
    private static EnvironmentConfig rwEnvConfig = buildRWEnvConfig();
    private static DatabaseConfig roDBConfig = buildRODBConfig();
    private static DatabaseConfig rwDBConfig = buildRWDBConfig();

    private final static String BDB_HOMES_ROOT = "/home/niq/.big/bdbs";

    public static Environment dbEnv(String title, boolean readOnly) {
        File envHome = new File(BDB_HOMES_ROOT + "/" + title);
        EnvironmentConfig envConfig = readOnly ? roEnvConfig : rwEnvConfig;
        Environment result = new  Environment(envHome, envConfig);
        return result;
    }

    private static EnvironmentConfig buildROEnvConfig() {
        try {
            EnvironmentConfig result = new EnvironmentConfig();
            result.setReadOnly(true);
            result.setAllowCreate(false);
            result.setLockTimeout(0, TimeUnit.SECONDS);
            result.setLocking(false);
            result.setTransactional(false);
            return result;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static EnvironmentConfig buildRWEnvConfig() {
        try {
            EnvironmentConfig result = new EnvironmentConfig();
            result.setReadOnly(false);
            result.setAllowCreate(true);
            result.setLockTimeout(0, TimeUnit.SECONDS);
            result.setLocking(false);
            result.setTransactional(false);
            return result;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static DatabaseConfig buildRODBConfig() {
        try {
            DatabaseConfig result = new DatabaseConfig();
            result.setReadOnly(true);
            result.setAllowCreate(false);
            result.setSortedDuplicates(false);
            result.setDeferredWrite(true);
            result.setTransactional(false);
            return result;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static DatabaseConfig buildRWDBConfig() {
        try {
            DatabaseConfig result = new DatabaseConfig();
            result.setReadOnly(false);
            result.setAllowCreate(true);
            result.setSortedDuplicates(false);
            result.setDeferredWrite(true);
            result.setTransactional(false);
            return result;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static String getDBName(int beatNumber) {
        StringBuilder result = new StringBuilder("beat_");

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

    public static Database getDB(String title, int beatNumber, boolean readOnly) {
        DatabaseConfig dbConfig = 
            readOnly ? BDBScoredBeat.roDBConfig : BDBScoredBeat.rwDBConfig;
        Environment dbEnv = BDBScoredBeat.dbEnv(title, readOnly);
        return dbEnv.openDatabase(null, BDBScoredBeat.getDBName(beatNumber), dbConfig);
    }

    public BDBScoredBeat(String title, int beatNumber) {
        super();

        this.database = BDBScoredBeat.getDB(title, beatNumber, true);
    }

    public void addScore(BandState bandState, int score) {
        /*
        DatabaseEntry key = IntegerBinding.intToEntry(bandState.serializedData());
        DatabaseEntry value = IntegerBinding.intToEntry(score);
        this.database.put(null, key, value);
        */
        
        // should never happen!
    }

    public int getScore(BandState bandState) {
        DatabaseEntry key = new DatabaseEntry();
        IntegerBinding.intToEntry(bandState.serializedData(), key);
        DatabaseEntry result = new DatabaseEntry();
        OperationStatus status = this.database.get(null, key, result, null);
        if (OperationStatus.NOTFOUND.equals(status)) {
            System.out.println("ruh-roh! no entry for\n" + bandState);
            // ruh-roh!
        }

        return IntegerBinding.entryToInt(result);
    }

    public void flush(String title, int beatNumber) {
    }

    public void close() {
        this.database.close();
    }

}
