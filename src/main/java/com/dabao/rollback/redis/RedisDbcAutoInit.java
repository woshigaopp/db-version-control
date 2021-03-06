package com.dabao.rollback.redis;

import com.dabao.rollback.AbstractRollBackInit;
import com.dabao.rollback.ConfigTree;
import com.dabao.rollback.DbContainer;
import com.dabao.rollback.GlobalRollBackConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.Pool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

@Configuration
public class RedisDbcAutoInit extends AbstractRollBackInit {

    @Autowired
    private GlobalRollBackConfig globalRollBackConfig;

    public RedisDbcAutoInit() {
        super("redis");
    }

    @Override
    public DbContainer initDbc(ConfigTree ct) {
        Pool<Jedis> pool = null;
        try {
            pool = parsePool(ct);
        } catch (Exception e) {
            e.printStackTrace();
        }
        RedisDbcConfig config = new RedisDbcConfig(pool);
        return new RedisDbContainer(config, globalRollBackConfig);
    }

    private Pool<Jedis> parsePool(ConfigTree ct) {
        try {
            GenericObjectPoolConfig poolConfig = parsePoolConfig(ct);

            String host = ct.getProperty("host", (String) null);
            int port = Integer.parseInt(ct.getProperty("port", "0"));
            int timeout = Integer.parseInt(ct.getProperty("timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT)));
            String password = ct.getProperty("password", (String) null);
            int database = Integer.parseInt(ct.getProperty("database", String.valueOf(Protocol.DEFAULT_DATABASE)));
            String clientName = ct.getProperty("clientName", (String) null);
            boolean ssl = Boolean.parseBoolean(ct.getProperty("ssl", "false"));

            String masterName = ct.getProperty("masterName", (String) null);
            String sentinels = ct.getProperty("sentinels", (String) null);//ip1:port,ip2:port

            Pool<Jedis> jedisPool;
            if (sentinels == null) {
                Objects.requireNonNull(host, "host/port or sentinels/masterName is required");
                if (port == 0) {
                    throw new IllegalStateException("host/port or sentinels/masterName is required");
                }
                jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database, clientName, ssl);
            } else {
                Objects.requireNonNull(masterName, "host/port or sentinels/masterName is required");
                String[] strings = sentinels.split(",");
                HashSet<String> sentinelsSet = new HashSet<>();
                for (String s : strings) {
                    if (s != null && !s.trim().equals("")) {
                        sentinelsSet.add(s.trim());
                    }
                }
                jedisPool = new JedisSentinelPool(masterName, sentinelsSet, poolConfig, timeout, password, database,
                        clientName);
            }
            return jedisPool;
        } catch (Exception e) {
            System.out.println("rollback exception");
            e.printStackTrace();
        }
        return null;
    }

    private GenericObjectPoolConfig parsePoolConfig(ConfigTree ct) throws Exception {
        // Spring Boot 2.0 removed RelaxedDataBinder class. Binder class not exists in 1.X
        if (ClassUtils.isPresent("org.springframework.boot.context.properties.bind.Binder",
                this.getClass().getClassLoader())) {
            // Spring Boot 2.0+
            String prefix = ct.subTree("poolConfig").getPrefix().toLowerCase();

            // invoke following code by reflect
            // Binder binder = Binder.get(environment);
            // return binder.bind(name, Bindable.of(GenericObjectPoolConfig.class)).get();

            Class<?> binderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
            Class<?> bindableClass = Class.forName("org.springframework.boot.context.properties.bind.Bindable");
            Class<?> bindResultClass = Class.forName("org.springframework.boot.context.properties.bind.BindResult");
            Method getMethodOnBinder = binderClass.getMethod("get", Environment.class);
            Method getMethodOnBindResult = bindResultClass.getMethod("get");
            Method bindMethod = binderClass.getMethod("bind", String.class, bindableClass);
            Method ofMethod = bindableClass.getMethod("of", Class.class);
            Object binder = getMethodOnBinder.invoke(null, environment);
            Object bindable = ofMethod.invoke(null, GenericObjectPoolConfig.class);
            Object bindResult = bindMethod.invoke(binder, prefix, bindable);
            return (GenericObjectPoolConfig) getMethodOnBindResult.invoke(bindResult);
        } else {
            // Spring Boot 1.X
            GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
            Map<String, Object> props = ct.subTree("poolConfig.").getProperties();

            // invoke following code by reflect
            //RelaxedDataBinder binder = new RelaxedDataBinder(poolConfig);
            //binder.bind(new MutablePropertyValues(props));

            Class<?> relaxedDataBinderClass = Class.forName("org.springframework.boot.bind.RelaxedDataBinder");
            Class<?> mutablePropertyValuesClass = Class.forName("org.springframework.beans.MutablePropertyValues");
            Constructor<?> c1 = relaxedDataBinderClass.getConstructor(Object.class);
            Constructor<?> c2 = mutablePropertyValuesClass.getConstructor(Map.class);
            Method bindMethod = relaxedDataBinderClass.getMethod("bind", PropertyValues.class);
            Object binder = c1.newInstance(poolConfig);
            bindMethod.invoke(binder, c2.newInstance(props));
            return poolConfig;
        }
    }
}
