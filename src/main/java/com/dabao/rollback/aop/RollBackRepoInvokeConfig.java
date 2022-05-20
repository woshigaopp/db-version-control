package com.dabao.rollback.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RollBackRepoInvokeConfig {

    private static final RollBackRepoInvokeConfig noRollBackRepoInvokeConfig = new RollBackRepoInvokeConfig();

    private static final RollBackRepoInvokeConfig notTargetMethodConfig = new RollBackRepoInvokeConfig();

    public static RollBackRepoInvokeConfig getNoRollBackRepoInvokeConfig() {
        return noRollBackRepoInvokeConfig;
    }

    public static RollBackRepoInvokeConfig getNotTargetMethodConfig() {
        return notTargetMethodConfig;
    }

    private List<String> ignoreProperties = new ArrayList<>();

    private Method updateByPrimaryKey;

    private Method selectByPrimaryKey;

    private Method updateByCondition;

    private Method selectByCondition;

    private Method primaryMethod;

    private Method updateByMultiKeys;

    private Method selectByMultiKeys;

    private Class condition;

    private Class updateEntity;

    private String primaryFieldName;

    private String versionField;

    private String primaryParameterName;

    private Class repoClass;

    public String getPrimaryParameterName() {
        return primaryParameterName;
    }

    public void setPrimaryParameterName(String primaryParameterName) {
        this.primaryParameterName = primaryParameterName;
    }

    public Method getUpdateByPrimaryKey() {
        return updateByPrimaryKey;
    }

    public void setUpdateByPrimaryKey(Method updateByPrimaryKey) {
        this.updateByPrimaryKey = updateByPrimaryKey;
    }

    public Method getSelectByPrimaryKey() {
        return selectByPrimaryKey;
    }

    public void setSelectByPrimaryKey(Method selectByPrimaryKey) {
        this.selectByPrimaryKey = selectByPrimaryKey;
    }

    public Method getUpdateByCondition() {
        return updateByCondition;
    }

    public void setUpdateByCondition(Method updateByCondition) {
        this.updateByCondition = updateByCondition;
    }

    public Method getSelectByCondition() {
        return selectByCondition;
    }

    public void setSelectByCondition(Method selectByCondition) {
        this.selectByCondition = selectByCondition;
    }

    public Class getCondition() {
        return condition;
    }

    public void setCondition(Class condition) {
        this.condition = condition;
    }

    public Class getRepoClass() {
        return repoClass;
    }

    public void setRepoClass(Class repoClass) {
        this.repoClass = repoClass;
    }

    public String getPrimaryFieldName() {
        return primaryFieldName;
    }

    public void setPrimaryFieldName(String primaryFieldName) {
        this.primaryFieldName = primaryFieldName;
    }

    public Class getUpdateEntity() {
        return updateEntity;
    }

    public void setUpdateEntity(Class updateEntity) {
        this.updateEntity = updateEntity;
    }

    public List<String> getIgnoreProperties() {
        return ignoreProperties;
    }

    public void setIgnoreProperties(List<String> ignoreProperties) {
        this.ignoreProperties = ignoreProperties;
    }

    public Method getPrimaryMethod() {
        return primaryMethod;
    }

    public void setPrimaryMethod(Method primaryMethod) {
        this.primaryMethod = primaryMethod;
    }

    public Method getUpdateByMultiKeys() {
        return updateByMultiKeys;
    }

    public void setUpdateByMultiKeys(Method updateByMultiKeys) {
        this.updateByMultiKeys = updateByMultiKeys;
    }

    public Method getSelectByMultiKeys() {
        return selectByMultiKeys;
    }

    public void setSelectByMultiKeys(Method selectByMultiKeys) {
        this.selectByMultiKeys = selectByMultiKeys;
    }

    public String getVersionField() {
        return versionField;
    }

    public void setVersionField(String versionField) {
        this.versionField = versionField;
    }
}
