package com.dabao.rollback;

import java.util.Map;

public class GlobalRollBackConfig {

    private Map<String, String> tableConfig;

    public Map<String, String> getTableConfig() {
        return tableConfig;
    }

    public void setTableConfig(Map<String, String> tableConfig) {
        this.tableConfig = tableConfig;
    }
}
