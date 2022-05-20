package com.dabao.rollback.aop;

import com.alibaba.ihome.rollback.anno.*;
import com.dabao.rollback.exception.RollbackNotConfigException;
import com.dabao.rollback.anno.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RollBackConfigUtils {

    public static RollBackEntryAnnoConfig parseRollBackMethod(Method method) {
        RollBackEntry anno = method.getAnnotation(RollBackEntry.class);

        if (anno == null) {
            return null;
        }

        RollBackEntryAnnoConfig rollBackEntryAnnoConfig = new RollBackEntryAnnoConfig();
        rollBackEntryAnnoConfig.setType(anno.type());
        rollBackEntryAnnoConfig.setTag(anno.tag());
        rollBackEntryAnnoConfig.setExpire(anno.expire());
        rollBackEntryAnnoConfig.setTimeUnit(anno.timeUnit());
        return rollBackEntryAnnoConfig;
    }

    public static boolean parse(RollBackInvokeConfig ric, Method method) {
        boolean hasAnnotation = false;

        RollBackEntryAnnoConfig rollBackEntryAnnoConfig = parseRollBackMethod(method);
        if (rollBackEntryAnnoConfig != null) {
            ric.setRollBackEntryAnnoConfig(rollBackEntryAnnoConfig);
            hasAnnotation = true;
        }
        return hasAnnotation;
    }

    public static boolean parse(RollBackRepoInvokeConfig ric, Method method) {

        ric.setRepoClass(method.getDeclaringClass());
        boolean updateByPrimaryKeyPresent = method.isAnnotationPresent(UpdateByPrimaryKey.class);
        boolean updateByConditionPresent = method.isAnnotationPresent(UpdateByCondition.class);
        boolean selectByPrimaryKeyPresent = method.isAnnotationPresent(SelectByPrimaryKey.class);
        boolean selectByConditionPresent = method.isAnnotationPresent(SelectByCondition.class);
        boolean updateMultiKeyPresent = method.isAnnotationPresent(UpdateByMultiKeys.class);
        boolean selectMultiKeyPresent = method.isAnnotationPresent(SelectByMultiKeys.class);

        if (updateMultiKeyPresent) {
            ric.setUpdateByMultiKeys(method);
        }

        if (selectMultiKeyPresent) {
            ric.setSelectByMultiKeys(method);
        }

        if (updateByPrimaryKeyPresent) {
            ric.setUpdateByPrimaryKey(method);

            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(UpdateEntity.class)) {
                    ric.setUpdateEntity(parameter.getType());
                }
                if (parameter.isAnnotationPresent(RollBackPrimaryKey.class)) {
                    ric.setPrimaryParameterName(parameter.getName());
                }
            }

            Class updateEntity = ric.getUpdateEntity();
            if (ric.getUpdateEntity() == null) {
                updateEntity = method.getParameters()[0].getType();
            }
            Field[] fields = updateEntity.getDeclaredFields();
            Method[] methods = updateEntity.getDeclaredMethods();

            for (Field field : fields) {
                Annotation primary = field.getAnnotation(RollBackPrimaryKey.class);
                Annotation ignoreProperties = field.getAnnotation(IgnoreProperties.class);
                Annotation version = field.getAnnotation(Version.class);
                if (primary != null) {
                    ric.setPrimaryFieldName(field.getName());
                }
                if (ignoreProperties != null) {
                    ric.getIgnoreProperties().add(field.getName());
                }
                if (version != null) {
                    ric.setVersionField(field.getName());
                }
            }

            for (Method m : methods) {
                Annotation primary = m.getAnnotation(RollBackPrimaryKey.class);
                if (primary != null) {
                    ric.setPrimaryMethod(m);
                }
            }

            if (ric.getPrimaryFieldName() == null && ric.getPrimaryMethod() == null && ric.getPrimaryParameterName()
                    == null) {
                throw new RollbackNotConfigException("primary key not config");
            }
            return true;
        }

        if (updateByConditionPresent) {
            ric.setUpdateByCondition(method);
            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(Condition.class)) {
                    ric.setCondition(parameter.getType());
                }
                if (parameter.isAnnotationPresent(UpdateEntity.class)) {
                    ric.setUpdateEntity(parameter.getType());
                }
            }
            return true;
        }

        if (selectByPrimaryKeyPresent) {
            ric.setSelectByPrimaryKey(method);
            return false;
        }

        if (selectByConditionPresent) {
            ric.setSelectByCondition(method);
            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(Condition.class)) {
                    ric.setCondition(parameter.getType());
                }
            }
            return false;
        }

        return false;
    }
}
