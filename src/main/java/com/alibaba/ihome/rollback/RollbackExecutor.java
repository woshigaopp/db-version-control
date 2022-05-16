package com.alibaba.ihome.rollback;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.ihome.rollback.anno.MultiKeyList;
import com.alibaba.ihome.rollback.anno.MultiResultMap;
import com.alibaba.ihome.rollback.anno.RollBackPrimaryKey;
import com.alibaba.ihome.rollback.anno.UpdateEntity;
import com.alibaba.ihome.rollback.aop.ConfigMap;
import com.alibaba.ihome.rollback.aop.RollBackRepoInvokeConfig;
import com.alibaba.ihome.rollback.aop.TraceSign;
import com.alibaba.ihome.rollback.exception.RollbackInterceptException;
import com.alibaba.ihome.rollback.redis.RedisDbContainer;
import com.alibaba.ihome.rollback.utils.ClassCompareUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alibaba.ihome.rollback.aop.RollBackRepoInterceptor.getPropertyValue;

@Configuration
public class RollbackExecutor implements ApplicationContextAware {

    private static Logger logger = LoggerFactory.getLogger(RollbackExecutor.class);

    @Autowired
    protected DbControlContainerBean dbControlContainerBean;

    @Autowired
    private ConfigMap configMap;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void rollBack(String tag, String dbType) {
        TraceSign.setIsFromRollBack();
        if (dbType == null) {
            dbType = "redis";
        }
        DbContainer dbContainer = dbControlContainerBean.getDbContainerMap().get(dbType);

        if (!dbContainer.getLock(tag)) {
            return;
        }
        try {
            while (dbContainer.hasData(tag)) {
                try {
                    RedisDbContainer.RollBackRecord rollBackRecord = dbContainer.rollBack(tag);
                    String repoBean = rollBackRecord.getRepoBean();
                    String repoClass = rollBackRecord.getRepoClassName();
                    Object recordKey = rollBackRecord.getKey();
                    Object newValue = rollBackRecord.getNewValue();
                    Object oldValue = rollBackRecord.getOldValue();
                    System.out.println("开始回滚 key :" + JSONObject.toJSONString(recordKey));
                    if (!applicationContext.containsBean(repoBean)) {
                        throw new RollbackInterceptException("repoBean is null");
                    }
                    Object repoBeanInstance = applicationContext.getBean(repoBean);
                    RollBackRepoInvokeConfig rrc = configMap.getRepoClassMap().get(repoClass);

                    if (rrc == null) {
                        throw new RollbackInterceptException("RollBackRepoInvokeConfig is null");
                    }
                    Method selectByPrimaryKey = rrc.getSelectByPrimaryKey();
                    Method selectByMultiKeys = rrc.getSelectByMultiKeys();
                    Object nowRecord;
                    if (selectByMultiKeys != null) {
                        nowRecord = selectByMultiKeys(recordKey, newValue, repoBeanInstance, rrc);
                    } else if (selectByPrimaryKey != null) {
                        nowRecord = selectByPrimaryKey.invoke(repoBeanInstance, recordKey);
                    } else {
                        throw new RollbackInterceptException("select method is null");
                    }

                    if (rrc.getVersionField() != null) {
                        Object nowVersion = getPropertyValue(nowRecord, rrc.getVersionField());
                        Object newVersion = getPropertyValue(newValue, rrc.getVersionField());
                        if (nowVersion != null && nowVersion.equals(newVersion)) {
                            Field[] fields = oldValue.getClass().getDeclaredFields();
                            for (int i =0; i< fields.length; i++) {
                                if (fields[i].getName().equals(rrc.getVersionField())) {
                                    fields[i].setAccessible(true);
                                    updateByPrimaryKey(recordKey, oldValue, repoBeanInstance, rrc);
                                    break;
                                } else {
                                    System.out.println("回滚记录 ID 冲突 key :" + JSONObject.toJSONString(recordKey));
                                }
                            }
                        } else {
                            System.out.println("回滚记录 ID 冲突 key :" + JSONObject.toJSONString(recordKey));
                        }
                    } else {
                        ClassCompareUtil.CompareResult compareResult = ClassCompareUtil.compareObject(nowRecord, newValue, rrc.getIgnoreProperties());

                        if (compareResult.isEqual()) {
                            if (rrc.getUpdateByPrimaryKey() != null) {
                                updateByPrimaryKey(recordKey, oldValue, repoBeanInstance, rrc);
                            } else if (rrc.getUpdateByMultiKeys() != null) {
                                updateByMultiKeys(recordKey, oldValue, repoBeanInstance, rrc);
                            }
                        } else {
                            System.out.println("回滚记录 ID 冲突 key :" + JSONObject.toJSONString(recordKey));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("roll back error");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            throw new RollbackInterceptException(e.getMessage());
        } finally {
            TraceSign.removeIsFromRollBack();
            dbContainer.unlock(tag);
        }
    }

    private Object selectByMultiKeys(Object recordKey, Object newValue, Object repoBeanInstance,
                                     RollBackRepoInvokeConfig rrc)
            throws IllegalAccessException, InvocationTargetException {
        Object nowRecord;
        List<Object> multiKeys = new ArrayList<>();
        if (newValue instanceof Map) {
            multiKeys.addAll(((Map) newValue).keySet());
        } else {
            throw new RollbackInterceptException("MultiResultMap is not a config on map");
        }

        Method selectByMultiKeysMethod = rrc.getSelectByMultiKeys();
        Parameter[] selectParameters = selectByMultiKeysMethod.getParameters();
        if (selectParameters.length != 2) {
            throw new RollbackInterceptException("selectMultiKeys method args length not match");
        }
        if (selectParameters[0].isAnnotationPresent(RollBackPrimaryKey.class) && selectParameters[1].isAnnotationPresent(
                MultiKeyList.class)) {
            nowRecord = rrc.getSelectByMultiKeys().invoke(repoBeanInstance, recordKey, multiKeys);
        } else if (selectParameters[0].isAnnotationPresent(MultiKeyList.class) && selectParameters[1].isAnnotationPresent(RollBackPrimaryKey.class)) {
            nowRecord = rrc.getSelectByMultiKeys().invoke(repoBeanInstance, multiKeys, recordKey);
        } else {
            throw new RollbackInterceptException("RollBackPrimaryKey or MultiKeyList not config");
        }
        return nowRecord;
    }

    private void updateByMultiKeys(Object recordKey, Object oldValue, Object repoBeanInstance,
                                   RollBackRepoInvokeConfig rrc)
            throws IllegalAccessException, InvocationTargetException {
        Parameter[] selectParameters = rrc.getUpdateByMultiKeys().getParameters();
        if (selectParameters.length == 2){
            if (selectParameters[0].isAnnotationPresent(RollBackPrimaryKey.class) && selectParameters[1].isAnnotationPresent(
                    MultiResultMap.class)) {
                rrc.getUpdateByMultiKeys().invoke(repoBeanInstance, recordKey, oldValue);
            } else if (selectParameters[0].isAnnotationPresent(MultiResultMap.class) && selectParameters[1].isAnnotationPresent(RollBackPrimaryKey.class)) {
                rrc.getUpdateByMultiKeys().invoke(repoBeanInstance, oldValue, recordKey);
            } else {
                throw new RollbackInterceptException("RollBackPrimaryKey or MultiResultMap not config");
            }
        } else {
            throw new RollbackInterceptException("update parameter length error");
        }
    }

    private void updateByPrimaryKey(Object recordKey, Object oldValue, Object repoBeanInstance, RollBackRepoInvokeConfig rrc)
            throws IllegalAccessException, InvocationTargetException {
        Parameter[] parameters = rrc.getUpdateByPrimaryKey().getParameters();
        if (parameters.length == 1) {
            rrc.getUpdateByPrimaryKey().invoke(repoBeanInstance, oldValue);
        } else if (parameters.length == 2){
            if (parameters[0].isAnnotationPresent(RollBackPrimaryKey.class) && parameters[1].isAnnotationPresent(
                    UpdateEntity.class)) {
                rrc.getUpdateByPrimaryKey().invoke(repoBeanInstance, recordKey, oldValue);
            } else if (parameters[0].isAnnotationPresent(UpdateEntity.class) && parameters[1].isAnnotationPresent(RollBackPrimaryKey.class)) {
                rrc.getUpdateByPrimaryKey().invoke(repoBeanInstance, oldValue, recordKey);
            } else {
                throw new RollbackInterceptException("RollBackPrimaryKey or UpdateEntity not config");
            }
        } else {
            throw new RollbackInterceptException("update parameter length error");
        }
    }

    private void removeIgnoreProperties(Object o, List<String> ignoreProperties) {
        Class clazz = o.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if (ignoreProperties.contains(name)) {
                field.setAccessible(true);
                try {
                    field.set(o, null);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
