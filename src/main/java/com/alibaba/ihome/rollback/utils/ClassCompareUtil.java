package com.alibaba.ihome.rollback.utils;

import com.alibaba.ihome.rollback.anno.CompareMethod;
import com.alibaba.ihome.rollback.exception.RollbackInterceptException;
import com.alibaba.ihome.rollback.support.KryoValueDecoder;
import com.alibaba.ihome.rollback.support.KryoValueEncoder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassCompareUtil {

    public static class CompareResult {
        private boolean isEqual;
        private Map<String, Map<String, Object>> diff;

        public CompareResult(boolean isEqual, Map<String, Map<String, Object>> diff) {
            this.isEqual = isEqual;
            this.diff = diff;
        }

        public CompareResult() {
        }

        public boolean isEqual() {
            return isEqual;
        }

        public void setEqual(boolean equal) {
            isEqual = equal;
        }

        public Map<String, Map<String, Object>> getDiff() {
            return diff;
        }

        public void setDiff(Map<String, Map<String, Object>> diff) {
            this.diff = diff;
        }
    }

    public static CompareResult compareObject(Object oldObject, Object newObject, List<String> ignoreProperties) {
        Method[] methods = oldObject.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(CompareMethod.class)) {
                try {
                    boolean equal = (boolean) method.invoke(oldObject, newObject);
                    CompareResult compareResult = new CompareResult();
                    compareResult.setEqual(equal);
                    return compareResult;
                } catch (Exception e) {
                    throw new RollbackInterceptException("compare Method args wrong");
                }
            }
        }
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        try {
            compareFields(resultMap, oldObject, newObject, ignoreProperties);
        } catch (Exception e) {
            e.printStackTrace();
            CompareResult compareResult = new CompareResult();
            compareResult.setEqual(false);
            return compareResult;
        }

        CompareResult compareResult = new CompareResult();
        if (resultMap.isEmpty()) {
            compareResult.setEqual(true);
        } else {
            compareResult.setEqual(false);
            compareResult.setDiff(resultMap);
        }
        return compareResult;
    }

    @SuppressWarnings("rawtypes")
    public static void compareFields(Map<String, Map<String, Object>> map, Object oldObject, Object newObject, List<String> ignoreProperties)
            throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        if (oldObject.getClass() != newObject.getClass()) {
            throw new RollbackInterceptException("not equal type");
        }
        Class clazz = oldObject.getClass();
        //获取object的所有属性
        PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors();
        if (pds.length != 0) {
            for (PropertyDescriptor pd : pds) {
                //遍历获取属性名
                String name = pd.getName();
                if (ignoreProperties.contains(name)) {
                    continue;
                }

                //获取属性的get方法
                Method readMethod = pd.getReadMethod();
                if (readMethod == null) {
                    continue;
                }
                readMethod.setAccessible(true);
                // 在oldObject上调用get方法等同于获得oldObject的属性值
                Object oldValue = readMethod.invoke(oldObject);
                // 在newObject上调用get方法等同于获得newObject的属性值
                Object newValue = readMethod.invoke(newObject);

                if (newValue == null) {
                    continue;
                }

                byte[] oldBytes = KryoValueEncoder.INSTANCE.apply(oldValue);
                byte[] newBytes = KryoValueEncoder.INSTANCE.apply(newValue);

                boolean equal = true;
                if (oldBytes.length != newBytes.length) {
                    equal = false;
                } else {
                    for (int i = 0; i< oldBytes.length; i ++) {
                        if (!(oldBytes[i] == newBytes[i])) {
                            equal = false;
                        }
                    }
                }

                if (!equal) {
                    Map<String, Object> valueMap = new HashMap<>();
                    valueMap.put("oldValue", KryoValueDecoder.INSTANCE.apply(oldBytes));
                    valueMap.put("newValue", KryoValueDecoder.INSTANCE.apply(newBytes));
                    map.put(name, valueMap);
                }
            }
        }
    }

}
