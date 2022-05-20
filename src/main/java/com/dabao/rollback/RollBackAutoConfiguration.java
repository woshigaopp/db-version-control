package com.dabao.rollback;

import com.dabao.rollback.redis.RedisDbcAutoInit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(RollBackProperties.class)
@Import({
        RedisDbcAutoInit.class
})
public class RollBackAutoConfiguration {

    private DbControlContainerBean dbControlContainerBean = new DbControlContainerBean();

    @Bean
    public DbControlContainerBean dbControlContainer() {
        return dbControlContainerBean;
    }

    @Bean
    public GlobalRollBackConfig globalRollBackConfig(RollBackProperties rollBackProperties) {
        GlobalRollBackConfig globalRollBackConfig = new GlobalRollBackConfig();
        globalRollBackConfig.setTableConfig(parseTableConfig(rollBackProperties.getTableConfig()));
        return globalRollBackConfig;
    }

    private Map<String, String> parseTableConfig(String config) {
        if (config == null) {
            return new HashMap<>();
        }
        String[] parts = config.split("\\|");
        Map<String, String> tableMap = new HashMap<>();
        for (String part : parts) {
            tableMap.put(part.split(":")[0], part.split(":")[1]);
        }
        return tableMap;
    }
}
