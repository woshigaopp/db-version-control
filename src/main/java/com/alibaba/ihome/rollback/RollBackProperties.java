package com.alibaba.ihome.rollback;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rollback")
public class RollBackProperties {

    private String tableConfig;

    public String getTableConfig() {
        return tableConfig;
    }

    public void setTableConfig(String tableConfig) {
        this.tableConfig = tableConfig;
    }
}
