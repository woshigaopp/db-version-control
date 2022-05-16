package com.alibaba.ihome.rollback.aop;

import com.alibaba.ihome.rollback.anno.EnableMethodRollBack;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 *
 * dabao
 */
@Configuration
public class RollBackProxyConfiguration implements ImportAware, ApplicationContextAware {

    protected AnnotationAttributes enableMethodCache;
    private ApplicationContext applicationContext;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableMethodCache = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableMethodRollBack.class.getName(), false));
        if (this.enableMethodCache == null) {
            throw new IllegalArgumentException(
                    "@EnableMethodRollBack is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean(name = RollBackAdvisor.ROLLBACK_ADVISOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public RollBackAdvisor rollBackAdvisor(RollBackEntryInterceptor rollBackEntryInterceptor) {
        RollBackAdvisor advisor = new RollBackAdvisor();
        advisor.setAdviceBeanName(RollBackAdvisor.ROLLBACK_ADVISOR_BEAN_NAME);
        advisor.setAdvice(rollBackEntryInterceptor);
        advisor.setBasePackages(this.enableMethodCache.getStringArray("basePackages"));
        return advisor;
    }

    @Bean(name = RollBackRepoAdvisor.ROLLBACK_REPO_ADVISOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public RollBackRepoAdvisor rollBackRepoAdvisor(RollBackRepoInterceptor rollBackRepoInterceptor) {
        RollBackRepoAdvisor advisor = new RollBackRepoAdvisor();
        advisor.setAdviceBeanName(RollBackRepoAdvisor.ROLLBACK_REPO_ADVISOR_BEAN_NAME);
        advisor.setAdvice(rollBackRepoInterceptor);
        advisor.setBasePackages(this.enableMethodCache.getStringArray("repoPackages"));
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public RollBackEntryInterceptor rollBackEntryInterceptor() {
        return new RollBackEntryInterceptor();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public RollBackRepoInterceptor rollBackRepoInterceptor() {
        return new RollBackRepoInterceptor();
    }
}