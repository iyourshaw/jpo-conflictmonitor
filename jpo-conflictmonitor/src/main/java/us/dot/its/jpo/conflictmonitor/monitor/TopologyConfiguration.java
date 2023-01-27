package us.dot.its.jpo.conflictmonitor.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import us.dot.its.jpo.conflictmonitor.ConflictMonitorProperties;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.StreamsTopology;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.validation.map.MapValidationAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.validation.map.MapValidationAlgorithmFactory;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.validation.map.MapValidationParameters;

@Configuration
public class TopologyConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TopologyConfiguration.class);

    @Bean
    public MapValidationAlgorithm mapValidationAlgorithmBean(@Autowired ConflictMonitorProperties conflictMonitorProps) {
        MapValidationAlgorithmFactory mapAlgoFactory = conflictMonitorProps.getMapValidationAlgorithmFactory();
        String mapAlgo = conflictMonitorProps.getMapValidationAlgorithm();
        MapValidationAlgorithm mapCountAlgo = mapAlgoFactory.getAlgorithm(mapAlgo);
        MapValidationParameters mapCountParams = conflictMonitorProps.getMapValidationParameters();
        logger.info("Map params {}", mapCountParams);
        if (mapCountAlgo instanceof StreamsTopology) {
            ((StreamsTopology)mapCountAlgo).setStreamsProperties(conflictMonitorProps.createStreamProperties("mapBroadcastRate"));
        }
        mapCountAlgo.setParameters(mapCountParams);
        return mapCountAlgo;
    }
    
}
