package com.alibaba.ihome.rollback;

import java.util.HashMap;
import java.util.Map;

public class DbControlContainerBean {
    private Map<String, DbContainer> dbContainerMap = new HashMap<>();

    public Map<String, DbContainer> getDbContainerMap() {
        return dbContainerMap;
    }
}
