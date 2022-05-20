package com.dabao.rollback.support;

import java.io.Serializable;

public final class CacheValueHolder<V> implements Serializable {
    private static final long serialVersionUID = -7973743507831565203L;
    private V value;
    private long expireTime;
    private long accessTime;

    /**
     * used by kyro
     */
    public CacheValueHolder() {
    }

    public CacheValueHolder(V value, long expireAfterWrite) {
        this.value = value;
        this.accessTime = System.currentTimeMillis();
        this.expireTime = accessTime + expireAfterWrite;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }
}

