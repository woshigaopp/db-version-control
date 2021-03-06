package com.dabao.rollback.anno;

import com.dabao.rollback.constant.RollBackConst;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RollBackEntry {
    String type() default RollBackConst.DEFAULT_TYPE;
    String tag();
    TimeUnit timeUnit() default TimeUnit.DAYS;
    long expire() default RollBackConst.DEFAULT_EXPIRE_DAYS;
}
