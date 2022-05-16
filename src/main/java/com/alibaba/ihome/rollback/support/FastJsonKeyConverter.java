/**
 * Created on  13-09-10 15:45
 */
package com.alibaba.ihome.rollback.support;

import com.alibaba.fastjson.JSON;

import java.util.function.Function;

/**
 dabao
 */
public class FastJsonKeyConverter implements Function<Object, Object> {

    public static final FastJsonKeyConverter INSTANCE = new FastJsonKeyConverter();

    @Override
    public String apply(Object originalKey) {
        if (originalKey == null) {
            return null;
        }
        if (originalKey instanceof String) {
            return (String) originalKey;
        }
        return JSON.toJSONString(originalKey);
    }

}

