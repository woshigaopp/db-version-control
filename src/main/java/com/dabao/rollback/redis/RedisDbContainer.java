package com.dabao.rollback.redis;

import com.alibaba.fastjson.JSONObject;
import com.dabao.rollback.DbContainer;
import com.dabao.rollback.GlobalRollBackConfig;
import com.dabao.rollback.support.KryoValueDecoder;
import com.dabao.rollback.support.KryoValueEncoder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collections;

public class RedisDbContainer implements DbContainer {

    private static final byte[] oldKey = new byte[0];

    private static final byte[] newKey = new byte[1];

    private RedisDbcConfig redisDbcConfig;

    private GlobalRollBackConfig globalRollBackConfig;

    private String lock_key = "redis_lock"; //锁键

    protected long internalLockLeaseTime = 1000 * 60 * 60;//锁过期时间

    private long timeout = 1000; //获取锁的超时时间

    SetParams params = SetParams.setParams().nx().px(internalLockLeaseTime);

    public RedisDbContainer(RedisDbcConfig redisDbcConfig, GlobalRollBackConfig globalRollBackConfig) {
        this.redisDbcConfig = redisDbcConfig;
        this.globalRollBackConfig = globalRollBackConfig;
    }

    public static class RecordKey {
        private String repoName;
        private String entryKey;
        private Object key;
        private long time;

        public RecordKey() {
        }

        public RecordKey(String repoName, String entryKey, Object key, long time) {
            this.repoName = repoName;
            this.entryKey = entryKey;
            this.key = key;
            this.time = time;
        }

        public String getRepoName() {
            return repoName;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

        public Object getKey() {
            return key;
        }

        public void setKey(Object key) {
            this.key = key;
        }

        public String getEntryKey() {
            return entryKey;
        }

        public void setEntryKey(String entryKey) {
            this.entryKey = entryKey;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

    public static class RollBackRecord {
        private String repoBean;
        private Object key;
        private String repoClassName;
        private Object oldValue;
        private Object newValue;

        public Object getKey() {
            return key;
        }

        public void setKey(Object key) {
            this.key = key;
        }

        public String getRepoClassName() {
            return repoClassName;
        }

        public void setRepoClassName(String repoClassName) {
            this.repoClassName = repoClassName;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public void setOldValue(Object oldValue) {
            this.oldValue = oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public void setNewValue(Object newValue) {
            this.newValue = newValue;
        }

        public String getRepoBean() {
            return repoBean;
        }

        public void setRepoBean(String repoBean) {
            this.repoBean = repoBean;
        }
    }

    @Override
    public void saveRecords(String repoBean, String tag, String repoClassName, String entryKey, Object repoInstance, Object key, Object oldValue, Object newValue) {
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        try {
            RecordKey recordKey = new RecordKey(repoBean, entryKey, key, System.currentTimeMillis());
            resource.set(repoBean, repoClassName);
            resource.rpush(tag.getBytes(), KryoValueEncoder.INSTANCE.apply(recordKey));
            resource.hset(KryoValueEncoder.INSTANCE.apply(recordKey), oldKey, KryoValueEncoder.INSTANCE.apply(oldValue));
            resource.hset(KryoValueEncoder.INSTANCE.apply(recordKey), newKey, KryoValueEncoder.INSTANCE.apply(newValue));
        } finally {
            resource.close();
        }
    }

    private void completeNewValue(Object oldValue, Object newValue) {
        try {
            Class clazz = newValue.getClass();
            PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors();

            for (PropertyDescriptor pd : pds) {

                //获取属性的get方法
                Method readMethod = pd.getReadMethod();
                Method writeMethod = pd.getWriteMethod();
                if (readMethod == null) {
                    continue;
                }
                readMethod.setAccessible(true);
                // 在oldObject上调用get方法等同于获得oldObject的属性值
                Object oldProperty = readMethod.invoke(oldValue);
                // 在newObject上调用get方法等同于获得newObject的属性值
                Object newProperty = readMethod.invoke(newValue);

                writeMethod.setAccessible(true);
                if (newProperty == null && oldProperty != null) {
                    writeMethod.invoke(newValue, oldProperty);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RollBackRecord rollBack(String tag) {
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        try {
            byte[] bytes = resource.rpop(tag.getBytes());
            RecordKey recordKey = (RecordKey) KryoValueDecoder.INSTANCE.apply(bytes);
            String repoClassName = resource.get(recordKey.getRepoName());

            byte[] oldValue = resource.hget(bytes, oldKey);
            byte[] newValue = resource.hget(bytes, newKey);

            RollBackRecord rollBackRecord = new RollBackRecord();
            rollBackRecord.setKey(recordKey.getKey());
            rollBackRecord.setOldValue(KryoValueDecoder.INSTANCE.apply(oldValue));
            rollBackRecord.setNewValue(KryoValueDecoder.INSTANCE.apply(newValue));
            rollBackRecord.setRepoClassName(repoClassName);
            rollBackRecord.setRepoBean(recordKey.getRepoName());
            return rollBackRecord;
        } finally {
            resource.close();
        }
    }

    public boolean hasData(String tag) {
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        if (resource.llen(tag.getBytes()) > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void setTagMethodMap(String tag, String methodKey) {
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        resource.hset("tag", tag, methodKey);
    }

    @Override
    public String getMethodKeyByTag(String tag) {
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        return resource.hget("tag", tag);
    }

    @Override
    public boolean getLock(String id) {
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        Long start = System.currentTimeMillis();
        try{
            for(;;){
                //SET命令返回OK ，则证明获取锁成功
                String lock = resource.set(lock_key, id, params);
                if("OK".equals(lock)){
                    return true;
                }
                //否则循环等待，在timeout时间内仍未获取到锁，则获取失败
                long l = System.currentTimeMillis() - start;
                if (l>=timeout) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }finally {
            resource.close();
        }
    }

    @Override
    public boolean unlock(String id){
        Jedis resource = redisDbcConfig.getJedisPool().getResource();
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then" +
                        "   return redis.call('del',KEYS[1]) " +
                        "else" +
                        "   return 0 " +
                        "end";
        try {
            Object result = resource.eval(script, Collections.singletonList(lock_key),
                    Collections.singletonList(id));
            if("1".equals(result.toString())){
                return true;
            }
            return false;
        }finally {
            resource.close();
        }
    }
}

