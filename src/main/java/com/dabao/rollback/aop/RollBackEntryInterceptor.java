/**
 * Created on  13-09-18 20:33
 */
package com.dabao.rollback.aop;

import com.dabao.rollback.DbContainer;
import com.dabao.rollback.DbControlContainerBean;
import com.dabao.rollback.anno.RollBackTag;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.dabao.rollback.aop.RollBackEntryPointcut.getKey;

/**
 * dabao
 */
public class RollBackEntryInterceptor implements MethodInterceptor, ApplicationContextAware {

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
        Method method = invocation.getMethod();
        Object obj = invocation.getThis();
        RollBackInvokeConfig ric = null;
        String entryKey = null;
        if (obj != null) {
            entryKey = getKey(method, obj.getClass());
            TraceSign.setEntryKey(entryKey);
            ric  = configMap.getByMethodInfo(entryKey);
        }

        Object[] args = invocation.getArguments();
        Annotation[][]  annotations = method.getParameterAnnotations();
        int parameterIndex = -1;
        if (args.length > 0) {
            for (int i = 0; i < annotations.length; i++) {
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j] instanceof RollBackTag) {
                        parameterIndex = i;
                    }
                }
            }
        }

        if (ric == null || ric == RollBackInvokeConfig.getNoRollBackInvokeConfigInstance()) {
            return invocation.proceed();
        }

        DbContainer dbContainer =
                dbControlContainerBean.getDbContainerMap().get(ric.getRollBackEntryAnnoConfig().getType());
        if (parameterIndex > -1) {
            TraceSign.setTag(args[parameterIndex].toString());
            configMap.getTagMethodKeyMap().put(args[parameterIndex].toString(), entryKey);
            dbContainer.setTagMethodMap(args[parameterIndex].toString(), entryKey);
        } else {
            TraceSign.setTag(ric.getRollBackEntryAnnoConfig().getTag());
        }
        TraceSign.setIntercept();
        TraceSign.setDbContainer(dbContainer);
        Object object = invocation.proceed();

        TraceSign.clear();
        return object;
    }
}
