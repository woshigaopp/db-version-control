package com.dabao.rollback.aop;

public class RollBackInvokeConfig {

    private RollBackEntryAnnoConfig rollBackEntryAnnoConfig;

    private static final RollBackInvokeConfig noRollBackInvokeConfigInstance = new RollBackInvokeConfig();

    public static RollBackInvokeConfig getNoRollBackInvokeConfigInstance() {
        return noRollBackInvokeConfigInstance;
    }

    public RollBackEntryAnnoConfig getRollBackEntryAnnoConfig() {
        return rollBackEntryAnnoConfig;
    }

    public void setRollBackEntryAnnoConfig(RollBackEntryAnnoConfig rollBackEntryAnnoConfig) {
        this.rollBackEntryAnnoConfig = rollBackEntryAnnoConfig;
    }
}
