package com.alibaba.ihome.rollback.aop;

import com.alibaba.ihome.rollback.DbContainer;
import com.alibaba.ihome.rollback.DbControlContainerBean;
import com.alibaba.ihome.rollback.anno.*;
import com.alibaba.ihome.rollback.exception.RollbackInterceptException;
import com.alibaba.ihome.rollback.exception.RollbackNotConfigException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alibaba.ihome.rollback.aop.RollBackEntryPointcut.getKey;

public class RollBackRepoInterceptor implements MethodInterceptor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private ConfigMap configMap;

    @Autowired
    protected DbControlContainerBean dbControlContainerBean;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        String tag = TraceSign.getTag();
        boolean fromRollback = TraceSign.getIsFromRollBack();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//        if (stackTraceElements.length > 2) {
//            String callKey = generateKey(stackTraceElements[2]);
//            System.out.println("callKey :" + callKey);
//            if (TraceSign.containsRepoKey(callKey)) {
//                return invocation.proceed();
//            }
//        }
        if (tag == null | fromRollback) {
            return invocation.proceed();
        } else {
            Method method = invocation.getMethod();
            Object obj = invocation.getThis();
            RollBackRepoInvokeConfig ric = null;
            String key = null;
            if (obj != null) {
                key = getKey(method, obj.getClass());
                ric  = configMap.getRepoInfo(key);
            }

            String entryKey = TraceSign.getEntryKey();
            if (ric == null || ric == RollBackRepoInvokeConfig.getNoRollBackRepoInvokeConfig()) {
                throw new RollbackNotConfigException("repo not config");
            } else if (ric == RollBackRepoInvokeConfig.getNotTargetMethodConfig()) {
                return invocation.proceed();
            } else {
                DbContainer dbContainer = TraceSign.getDbContainer();
                if (dbContainer == null) {
                    throw new RollbackInterceptException("dbContainer is null");
                }

                String repoBeanName = applicationContext.getBeanNamesForType(method.getDeclaringClass())[0];
                Method actualMethod = configMap.getMethodKey(key);
                if (actualMethod != null && actualMethod.isAnnotationPresent(UpdateByCondition.class)) {
                    if (stackTraceElements.length > 1) {
                        String callKey = generateKey(stackTraceElements[1]);
                        TraceSign.addRepoKey(callKey);
                    }
                    Object[] args = invocation.getArguments();
                    Class condition = ric.getCondition();
                    Object oldValue = null;
                    Object primaryKey = null;
                    for (Object o : args) {
                        if (o.getClass() == condition) {
                            oldValue = ric.getSelectByCondition().invoke(invocation.getThis(), o);
                            if (ric.getPrimaryFieldName() != null) {
                                primaryKey = getPropertyValue(oldValue, ric.getPrimaryFieldName());
                            } else {
                                primaryKey = ric.getPrimaryMethod().invoke(oldValue);
                            }
                        }
                    }

                    Object ret = invocation.proceed();
                    Object newValue = ric.getSelectByPrimaryKey().invoke(invocation.getThis(), primaryKey);
                    dbContainer.saveRecords(repoBeanName, tag, actualMethod.getDeclaringClass().getName(), entryKey, invocation.getThis(), primaryKey, oldValue, newValue);
                    return ret;
                } else if (actualMethod != null && (actualMethod.isAnnotationPresent(UpdateByPrimaryKey.class) | actualMethod.isAnnotationPresent(ViceUpdateByPrimaryKey.class))) {
                    if (stackTraceElements.length > 1) {
                        String callKey = generateKey(stackTraceElements[1]);
                        TraceSign.addRepoKey(callKey);
                    }

                    Object updateValue = null;
                    Object[] args = invocation.getArguments();
                    Class updateEntity = ric.getUpdateEntity();
                    for (Object arg : args) {
                        if (arg.getClass() == updateEntity) {
                            updateValue = arg;
                        }
                    }
                    if (updateValue == null) {
                        throw new RollbackInterceptException("updateEntity not config exception");
                    }
                    Object primaryKey;
                    int i =-1;
                    Parameter[] parameters = actualMethod.getParameters();
                    for (int j = 0; j < parameters.length; j++) {
                        if (parameters[j].getName().equals(ric.getPrimaryParameterName())) {
                           i = j;
                        }
                    }

                    if (i > -1) {
                        primaryKey = args[i];
                    } else if (ric.getPrimaryFieldName() != null) {
                        primaryKey = getPropertyValue(updateValue, ric.getPrimaryFieldName());
                    } else {
                        primaryKey = ric.getPrimaryMethod().invoke(updateValue);
                    }
                    Object oldValue = ric.getSelectByPrimaryKey().invoke(invocation.getThis(), primaryKey);
                    Object ret = invocation.proceed();
                    Object newValue = ric.getSelectByPrimaryKey().invoke(invocation.getThis(), primaryKey);
                    dbContainer.saveRecords(repoBeanName, tag, actualMethod.getDeclaringClass().getName(), entryKey, invocation.getThis(), primaryKey, oldValue, newValue);
                    return ret;
                } else if (actualMethod != null && actualMethod.isAnnotationPresent(UpdateByMultiKeys.class)) {
                    if (stackTraceElements.length > 1) {
                        String callKey = generateKey(stackTraceElements[1]);
                        TraceSign.addRepoKey(callKey);
                    }

                    Object[] args = invocation.getArguments();
                    Object oldValue = null;
                    Object newValue = null;
                    Object primaryKey = null;

                    Parameter[] parameters = actualMethod.getParameters();
                    for(int n = 0; n < parameters.length; n++) {
                        if (parameters[n].isAnnotationPresent(RollBackPrimaryKey.class)) {
                            primaryKey = args[n];
                        }
                        if (parameters[n].isAnnotationPresent(MultiResultMap.class)) {
                            newValue = args[n];
                        }
                    }

                    List<Object> multiKeys = new ArrayList<>();
                    if (newValue instanceof Map) {
                        multiKeys.addAll(((Map) newValue).keySet());
                    } else {
                        throw new RollbackInterceptException("MultiResultMap is not a config on map");
                    }

                    Method selectByMultiKeysMethod = ric.getSelectByMultiKeys();
                    Parameter[] selectParameters = selectByMultiKeysMethod.getParameters();
                    if (selectParameters.length != 2) {
                        throw new RollbackInterceptException("selectMultiKeys method args length not match");
                    }
                    if (selectParameters[0].isAnnotationPresent(RollBackPrimaryKey.class) && selectParameters[1].isAnnotationPresent(MultiKeyList.class)) {
                        oldValue = ric.getSelectByMultiKeys().invoke(invocation.getThis(), primaryKey, multiKeys);
                    } else if (selectParameters[0].isAnnotationPresent(MultiKeyList.class) && selectParameters[1].isAnnotationPresent(RollBackPrimaryKey.class)) {
                        oldValue = ric.getSelectByMultiKeys().invoke(invocation.getThis(), multiKeys, primaryKey);
                    } else {
                        throw new RollbackInterceptException("RollBackPrimaryKey or MultiKeyList not config");
                    }

                    dbContainer.saveRecords(repoBeanName, tag, actualMethod.getDeclaringClass().getName(), entryKey, invocation.getThis(), primaryKey, oldValue, newValue);

                }
                return invocation.proceed();
            }

        }
    }

    public static Object getPropertyValue(Object obj, String propertyName) throws IllegalAccessException {
        Class<?> Clazz = obj.getClass();
        Field field = getField(Clazz, propertyName);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static Field getField(Class<?> clazz, String propertyName) {
        try {
            return clazz.getDeclaredField(propertyName);
        } catch (NoSuchFieldException e) {
            return getField(clazz.getSuperclass(), propertyName);
        }
    }

    private String generateKey(StackTraceElement s) {
        String methodName = s.getMethodName();
        String className = s.getClassName();
        int lineNumber = s.getLineNumber();
        return className + methodName + lineNumber;
    }
}
