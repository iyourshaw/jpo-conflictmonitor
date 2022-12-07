package us.dot.its.jpo.conflictmonitor.monitor;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import us.dot.its.jpo.conflictmonitor.KafkaConfiguration;

import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:10092", "port=10092" })
public class KafkaConfigurationTest {

    private final static Logger logger = LoggerFactory.getLogger(KafkaConfigurationTest.class);

    @Autowired
    private KafkaConfiguration kafkaConfig;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    public void testKafkaConfigurationInjected() {
        assertThat(kafkaConfig, notNullValue());
    }

    @Test
    public void testConfigurationsLoaded() {
        assertThat(kafkaConfig.getNumPartitions(), greaterThan(0));
        assertThat(kafkaConfig.getNumReplicas(), greaterThan(0));
        assertThat(kafkaConfig.getCreateTopics(), notNullValue());
    }

    @Test
    public void testKafkaAdminInjected() {
        assertThat(kafkaAdmin, notNullValue()); 
    }

    @Test
    public void testKafkaAdminHasBootstrapServerProperty() {
        var kProps = kafkaAdmin.getConfigurationProperties();
        assertThat(kProps, notNullValue());
        logger.info("KafkaAdmin exists: Props: {}", kProps);
        assertThat(kProps, hasKey("bootstrap.servers"));
        var servers = kProps.get("bootstrap.servers");
        assertThat(servers, instanceOf(List.class));
        assertThat((List<String>)servers, hasItem("localhost:10092"));
    }
    
}
