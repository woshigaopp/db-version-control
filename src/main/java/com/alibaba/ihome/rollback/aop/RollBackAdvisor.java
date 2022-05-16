/**
 * Created on  13-09-19 20:40
 */
package com.alibaba.ihome.rollback.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 */
public class RollBackAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    public static final String ROLLBACK_ADVISOR_BEAN_NAME = "rollback.internalAdvisor";

    private String[] basePackages;

    @Autowired
    private ConfigMap configMap;

    @Override
    public Pointcut getPointcut() {
        RollBackEntryPointcut pointcut = new RollBackEntryPointcut(basePackages);
        pointcut.setConfigMap(configMap);
        return pointcut;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }
}
