package com.dabao.rollback;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractRollBackInit implements InitializingBean {

    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected DbControlContainerBean dbControlContainerBean;

    protected String[] typeNames;

    private boolean initialized = false;

    public AbstractRollBackInit(String... cacheTypes) {
        Objects.requireNonNull(cacheTypes,"cacheTypes can't be null");
        Assert.isTrue(cacheTypes.length > 0, "cacheTypes length is 0");
        this.typeNames = cacheTypes;
    }

    @Override
    public void afterPropertiesSet() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    process("rollback.dbcContainer.", dbControlContainerBean.getDbContainerMap());
                    initialized = true;
                }
            }
        }
    }

    private void process(String prefix, Map dbcContainerMap) {
        ConfigTree resolver = new ConfigTree(environment, prefix);
        Map<String, Object> m = resolver.getProperties();
        Set<String> dbcContainerNames = resolver.directChildrenKeys();
        for (String dbcArea : dbcContainerNames) {
            final Object configType = m.get(dbcArea + ".type");
            boolean match = Arrays.stream(typeNames).anyMatch((tn) -> tn.equals(configType));
            if (!match) {
                continue;
            }
            ConfigTree ct = resolver.subTree(dbcArea + ".");
            DbContainer c = initDbc(ct);
            dbcContainerMap.put(configType, c);
        }
    }

    public abstract DbContainer initDbc(ConfigTree ct);
}
