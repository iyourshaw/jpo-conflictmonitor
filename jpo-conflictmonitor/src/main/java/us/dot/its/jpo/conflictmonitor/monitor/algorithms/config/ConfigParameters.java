package us.dot.its.jpo.conflictmonitor.monitor.algorithms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Generated;
import us.dot.its.jpo.conflictmonitor.monitor.models.config.ConfigData;

import static us.dot.its.jpo.conflictmonitor.monitor.models.config.UpdateType.READ_ONLY;

@Data
@Generated
@Component
@ConfigurationProperties(prefix = "config")
public class ConfigParameters {
    
    // Kafka topics
    @ConfigData(key = "config.defaultTableName",
        description = "Name of topic for default configuration GlobalKTable",
        updateType = READ_ONLY)
    String defaultTableName;

    @ConfigData(key = "config.defaultStateStore",
        description = "Default config state store name",
        updateType = READ_ONLY)
    String defaultStateStore;

    @ConfigData(key = "config.intersectionStateStore",
        description = "Intersection config state store name",
        updateType = READ_ONLY)
    String intersectionStateStore;

    @ConfigData(key = "config.intersectionTableName",
            description = "Name of topic for intersection GlobalKTable",
            updateType = READ_ONLY)
    String intersectionTableName;

}
