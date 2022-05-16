package com.alibaba.ihome.rollback.aop;

import java.util.concurrent.TimeUnit;

public class RollBackEntryAnnoConfig {

    private String type;

    private String tag;

    private long expire;

    private TimeUnit timeUnit;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
