package com.dabao.rollback.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;

public class RollBackRepoAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    public static final String ROLLBACK_REPO_ADVISOR_BEAN_NAME = "rollback.internalRepoAdvisor";

    private String[] basePackages;

    @Autowired
    private ConfigMap configMap;

    @Override
    public Pointcut getPointcut() {
        RollBackRepoPointcut pointcut = new RollBackRepoPointcut(basePackages);
        pointcut.setConfigMap(configMap);
        return pointcut;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }
}
