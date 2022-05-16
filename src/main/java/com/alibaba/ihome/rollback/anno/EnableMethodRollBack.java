package com.alibaba.ihome.rollback.anno;

import com.alibaba.ihome.rollback.aop.CommonConfig;
import com.alibaba.ihome.rollback.aop.ConfigSelector;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CommonConfig.class, ConfigSelector.class})
public @interface EnableMethodRollBack {

    /**
     * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
     * to standard Java interface-based proxies. The default is {@code false}. <strong>
     * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
     * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
     * Spring-managed beans requiring proxying, not just those marked with {@code @Cacheable}.
     * For example, other beans marked with Spring's {@code @Transactional} annotation will
     * be upgraded to subclass proxying at the same time. This approach has no negative
     * impact in practice unless one is explicitly expecting one type of proxy vs another,
     * e.g. in tests.
     */
    boolean proxyTargetClass() default false;

    /**
     * Indicate how caching advice should be applied. The default is
     * {@link AdviceMode#PROXY}.
     *
     * @see AdviceMode
     */
    AdviceMode mode() default AdviceMode.PROXY;


    String[] basePackages();


    String[] repoPackages();
}
