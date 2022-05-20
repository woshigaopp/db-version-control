/**
 * Created on 2018/1/22.
 */
package com.dabao.rollback.aop;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * dabao
 */
public class ConfigMap {
    private ConcurrentHashMap<String, RollBackInvokeConfig> methodInfoMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, RollBackRepoInvokeConfig> repoInfoMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, RollBackRepoInvokeConfig> repoClassMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, String> tagMethodKeyMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Method> methodKey = new ConcurrentHashMap<>();

    public void putByMethodInfo(String key, RollBackInvokeConfig config) {
        methodInfoMap.put(key, config);
    }

    public RollBackInvokeConfig getByMethodInfo(String key) {
        return methodInfoMap.get(key);
    }

    public void putRepoInfo(String key, RollBackRepoInvokeConfig config) {
        repoInfoMap.put(key, config);
    }

    public RollBackRepoInvokeConfig getRepoInfo(String key) {
        return repoInfoMap.get(key);
    }

    public void putMethodKey(String key, Method method) {
        methodKey.put(key, method);
    }

    public Method getMethodKey(String key) {
        return methodKey.get(key);
    }


    public ConcurrentHashMap<String, RollBackRepoInvokeConfig> getRepoClassMap() {
        return repoClassMap;
    }

    public ConcurrentHashMap<String, String> getTagMethodKeyMap() {
        return tagMethodKeyMap;
    }

    public void setTagMethodKeyMap(ConcurrentHashMap<String, String> tagMethodKeyMap) {
        this.tagMethodKeyMap = tagMethodKeyMap;
    }

    public ConcurrentHashMap<String, RollBackInvokeConfig> getMethodInfoMap() {
        return methodInfoMap;
    }

    public void setMethodInfoMap(
            ConcurrentHashMap<String, RollBackInvokeConfig> methodInfoMap) {
        this.methodInfoMap = methodInfoMap;
    }

    public ConcurrentHashMap<String, RollBackRepoInvokeConfig> getRepoInfoMap() {
        return repoInfoMap;
    }

    public void setRepoInfoMap(
            ConcurrentHashMap<String, RollBackRepoInvokeConfig> repoInfoMap) {
        this.repoInfoMap = repoInfoMap;
    }

    public void setRepoClassMap(
            ConcurrentHashMap<String, RollBackRepoInvokeConfig> repoClassMap) {
        this.repoClassMap = repoClassMap;
    }
}
