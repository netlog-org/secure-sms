package org.anhtn.securesms.utils;

import java.util.Hashtable;
import java.util.Objects;

public class CacheHelper {

    public static CacheHelper getInstance() {
        if (sInstance == null) {
            sInstance = new CacheHelper();
        }
        return sInstance;
    }

    private static CacheHelper sInstance;

    private Hashtable<String, Object> mData = new Hashtable<>();

    private CacheHelper() {}

    public synchronized boolean contains(String key) {
        return mData.containsKey(key);
    }

    public synchronized void put(String key, Object data) {
        mData.put(key, data);
    }

    public synchronized Object get(String key) {
        return mData.get(key);
    }

    public synchronized void remove(String key) {
        mData.remove(key);
    }

    public synchronized void clearCache() {
        mData.clear();
    }
}
