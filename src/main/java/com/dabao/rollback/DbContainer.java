package com.dabao.rollback;

import com.dabao.rollback.redis.RedisDbContainer;

public interface DbContainer {
    void saveRecords(String repoName, String tag, String repoClassName, String entryKey, Object repoInstance,
                     Object key,
                     Object oldValue,
                     Object newValue);
    RedisDbContainer.RollBackRecord rollBack(String entryKey);
    boolean hasData(String tag);
    boolean getLock(String id);
    boolean unlock(String id);
    void setTagMethodMap(String tag, String methodKey);
    String getMethodKeyByTag(String tag);
}