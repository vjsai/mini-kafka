package com.vjsai.mini.kafka.cache;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * A Cache implementation with a TimeToLive.If data has a TTL it will be deleted if specified time is exceeded.
 * @param <K>
 * @param <V>
 */
public class DataCache<K, V> {
    private final long mDefaultTTL = 15;
    private long mTTL = 0;
    private HashMap<K, DataValue<V>> dataMap;

    DataCache() {
        dataMap = new HashMap<K, DataValue<V>>();
        mTTL = mDefaultTTL;
    }

    DataCache(long ttlMinutes) {
        dataMap = new HashMap<K, DataValue<V>>();
        mTTL = ttlMinutes;
    }

    public void put(K key, V value) {
        dataMap.put(key, new DataValue<V>(value));
    }

    public V get(K key) {
        DataValue<V> data = dataMap.get(key);
        V result = null;
        if (data != null) {
            long t_diff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - data.lastAccessed);
            //if the time difference from lastAccessed is greater than Time to Live remove the key from cache.
            if (t_diff >= mTTL) {
                dataMap.remove(key);
                data.value = null;
            }
            result = data.value;
        }
        return result;
    }

    public boolean containsKey(K key) {
        if (dataMap.containsKey(key)) {
            this.get(key);
        }
        return dataMap.containsKey(key);
    }


    public void remove(K key) {
        if (dataMap.containsKey(key)) {
            dataMap.remove(key);
        }
    }

    public long getmDefaultTTL() {
        return mDefaultTTL;
    }

    public long getmTTL() {
        return mTTL;
    }

    private final class DataValue<T> {
        public T value;
        public long lastAccessed;

        DataValue(T value) {
            this.value = value;
            lastAccessed = System.currentTimeMillis();
        }
    }
}
