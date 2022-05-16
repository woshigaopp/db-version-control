package com.alibaba.ihome.rollback.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

public class RedisDbcConfig {

    public RedisDbcConfig(Pool<Jedis> jedisPool) {
        this.jedisPool = jedisPool;
    }

    private Pool<Jedis> jedisPool;

    public Pool<Jedis> getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(Pool<Jedis> jedisPool) {
        this.jedisPool = jedisPool;
    }
}
