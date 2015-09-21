package org.thanthoai.securesms.utils.cache;

import java.util.Hashtable;

public class CacheHelper {

    public synchronized static CacheHelper getInstance() {
        if (sInstance == null) {
            sInstance = new CacheHelper();
        }
        return sInstance;
    }

    private static CacheHelper sInstance;

    private final Hashtable<String, Object> mData = new Hashtable<>();

    private CacheHelper() {}

    public boolean contains(String key) {
        return mData.containsKey(key);
    }

    public void put(String key, Object data) {
        mData.put(key, data);
    }

    public Object get(String key) {
        return mData.get(key);
    }
}
