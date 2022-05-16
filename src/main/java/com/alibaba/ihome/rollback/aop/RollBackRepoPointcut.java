package com.alibaba.ihome.rollback.aop;

import com.alibaba.ihome.rollback.anno.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public class RollBackRepoPointcut extends StaticMethodMatcherPointcut implements ClassFilter {

    private static final Logger logger = LoggerFactory.getLogger(RollBackEntryPointcut.class);

    private ConfigMap configMap;

    private String[] basePackages;

    public RollBackRepoPointcut(String[] basePackages) {
        setClassFilter(this);
        this.basePackages = basePackages;
    }

    @Override
    public boolean matches(Class clazz) {
        boolean b = matchesImpl(clazz);
        logger.trace("check class match {}: {}", b, clazz);
        return b;
    }

    private boolean matchesImpl(Class clazz) {
        if (matchesThis(clazz)) {
            return true;
        }
        Class[] cs = clazz.getInterfaces();
        if (cs != null) {
            for (Class c : cs) {
                if (matchesImpl(c)) {
                    return true;
                }
            }
        }
        if (!clazz.isInterface()) {
            Class sp = clazz.getSuperclass();
            if (sp != null && matchesImpl(sp)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesThis(Class clazz) {
        String name = clazz.getName();
        if (exclude(name)) {
            return false;
        }
        boolean include = include(name);
        if (include) {
            parseByTargetClass(clazz);
        }
        return include;
    }

    private boolean include(String name) {
        if (basePackages != null) {
            for (String p : basePackages) {
                if (name.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean exclude(String name) {
        if (name.startsWith("java")) {
            return true;
        }
        if (name.startsWith("org.springframework")) {
            return true;
        }
        if (name.indexOf("$$EnhancerBySpringCGLIB$$") >= 0) {
            return true;
        }
        if (name.indexOf("$$FastClassBySpringCGLIB$$") >= 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean matches(Method method, Class targetClass) {
        String key = getKey(method, targetClass);
        boolean b = matchesImpl(method, targetClass, key);
        if (b) {
            if (logger.isDebugEnabled()) {
                logger.debug("check method match true: method={}, declaringClass={}, targetClass={}",
                        method.getName(),
                        ClassUtil.getShortClassName(method.getDeclaringClass().getName()),
                        targetClass == null ? null : ClassUtil.getShortClassName(targetClass.getName()));
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("check method match false: method={}, declaringClass={}, targetClass={}",
                        method.getName(),
                        ClassUtil.getShortClassName(method.getDeclaringClass().getName()),
                        targetClass == null ? null : ClassUtil.getShortClassName(targetClass.getName()));
            }
        }
        return b;
    }

    private boolean matchesImpl(Method method, Class targetClass, String key) {
        if (!matchesThis(method.getDeclaringClass())) {
            if (targetClass.getInterfaces().length != 0) {
                Class[] infs = targetClass.getInterfaces();
                for (Class inf : infs) {
                    try {
                        Method infMethod = inf.getMethod(method.getName(), method.getParameterTypes());
                        return matchesImpl(infMethod, inf, key);
                    } catch (NoSuchMethodException e) {
                        return false;
                    }
                }
            }
            return false;
        }
        if (exclude(targetClass.getName())) {
            return false;
        }

        if (configMap.getRepoInfo(key) != null) {
            return true;
        } else if (method.getDeclaringClass().getAnnotation(RollBackRepo.class) == null) {
            configMap.putRepoInfo(key, RollBackRepoInvokeConfig.getNoRollBackRepoInvokeConfig());
            return true;
        } else {
            boolean isUpdate = method.isAnnotationPresent(UpdateByPrimaryKey.class) | method.isAnnotationPresent(
                    UpdateByCondition.class) | method.isAnnotationPresent(UpdateByMultiKeys.class) | method.isAnnotationPresent(
                    ViceUpdateByPrimaryKey.class);

            if (!isUpdate) {
                configMap.putRepoInfo(key, RollBackRepoInvokeConfig.getNotTargetMethodConfig());
            } else {
                configMap.putRepoInfo(key, configMap.getRepoClassMap().get(method.getDeclaringClass().getName()));
                configMap.putMethodKey(key, method);
            }
        }

        return true;
    }

    private boolean parseByTargetClass(Class<?> clazz) {
        if (clazz.getAnnotation(RollBackRepo.class) != null) {
            Method[] methods =  clazz.getDeclaredMethods();
            RollBackRepoInvokeConfig rrc = new RollBackRepoInvokeConfig();
            for (Method method : methods) {
                RollBackConfigUtils.parse(rrc, method);
            }
            configMap.getRepoClassMap().put(clazz.getName(), rrc);
            return true;
        }

        return false;
    }

    public static String getKey(Method method, Class targetClass) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName());
        sb.append('.');
        sb.append(method.getName());
        sb.append(Type.getMethodDescriptor(method));
        if (targetClass != null) {
            sb.append('_');
            sb.append(targetClass.getName());
        }
        return sb.toString();
    }

    private boolean methodMatch(String name, Method method, Class<?>[] paramTypes) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (!name.equals(method.getName())) {
            return false;
        }
        Class<?>[] ps = method.getParameterTypes();
        if (ps.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < ps.length; i++) {
            if (!ps[i].equals(paramTypes[i])) {
                return false;
            }
        }
        return true;
    }

    public void setConfigMap(ConfigMap configMap) {
        this.configMap = configMap;
    }
}