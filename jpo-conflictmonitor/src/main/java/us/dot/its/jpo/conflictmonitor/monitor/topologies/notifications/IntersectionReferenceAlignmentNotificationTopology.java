package us.dot.its.jpo.conflictmonitor.monitor.topologies.notifications;

import static us.dot.its.jpo.conflictmonitor.monitor.algorithms.intersection_reference_alignment_notification.IntersectionReferenceAlignmentNotificationConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.conflictmonitor.monitor.algorithms.intersection_reference_alignment_notification.IntersectionReferenceAlignmentNotificationParameters;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.intersection_reference_alignment_notification.IntersectionReferenceAlignmentNotificationStreamsAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.IntersectionReferenceAlignmentEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.IntersectionReferenceAlignmentNotification;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.Notification;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.JsonSerdes;



import static us.dot.its.jpo.conflictmonitor.monitor.algorithms.lane_direction_of_travel_assessment.LaneDirectionOfTravelAssessmentConstants.*;

import java.util.Properties;



import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.conflictmonitor.monitor.algorithms.lane_direction_of_travel_assessment.LaneDirectionOfTravelAssessmentParameters;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.lane_direction_of_travel_assessment.LaneDirectionOfTravelAssessmentStreamsAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.LaneDirectionOfTravelAggregator;
import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.LaneDirectionOfTravelAssessment;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.LaneDirectionOfTravelEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.TimestampExtractors.LaneDirectionOfTravelTimestampExtractor;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.JsonSerdes;



@Component(DEFAULT_INTERSECTION_REFERENCE_ALIGNMENT_NOTIFICATION_ALGORITHM)
public class IntersectionReferenceAlignmentNotificationTopology 
    implements IntersectionReferenceAlignmentNotificationStreamsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(IntersectionReferenceAlignmentNotificationTopology.class);

    IntersectionReferenceAlignmentNotificationParameters parameters;
    Properties streamsProperties;
    Topology topology;
    KafkaStreams streams;

    @Override
    public void setParameters(IntersectionReferenceAlignmentNotificationParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public IntersectionReferenceAlignmentNotificationParameters getParameters() {
        return parameters;
    }

    @Override
    public void setStreamsProperties(Properties streamsProperties) {
       this.streamsProperties = streamsProperties;
    }

    @Override
    public Properties getStreamsProperties() {
        return streamsProperties;
    }

    @Override
    public KafkaStreams getStreams() {
        return streams;
    }

    @Override
    public void start() {
        if (parameters == null) {
            throw new IllegalStateException("Start called before setting parameters.");
        }
        if (streamsProperties == null) {
            throw new IllegalStateException("Streams properties are not set.");
        }
        if (streams != null && streams.state().isRunningOrRebalancing()) {
            throw new IllegalStateException("Start called while streams is already running.");
        }
        logger.info("StartingConnectionOfTravelAssessmentTopology");
        Topology topology = buildTopology();
        streams = new KafkaStreams(topology, streamsProperties);
        streams.start();
        logger.info("Started ConnectionOfTravelAssessmentTopology.");
        System.out.println("Started Events Topology");
    }

    public Topology buildTopology() {
        var builder = new StreamsBuilder();

        // // GeoJson Input Spat Stream
        KStream<String, IntersectionReferenceAlignmentEvent> intersectionReferenceAlignmentEvents = 
            builder.stream(
                parameters.getIntersectionReferenceAlignmentEventTopicName(), 
                Consumed.with(
                    Serdes.String(), 
                    JsonSerdes.IntersectionReferenceAlignmentEvent())
                );

        if(parameters.isDebug()){
            intersectionReferenceAlignmentEvents.print(Printed.toSysOut());
        }

        KStream<String, IntersectionReferenceAlignmentNotification> notificationEventStream = intersectionReferenceAlignmentEvents.flatMap(
            (key, value)->{
                List<KeyValue<String, IntersectionReferenceAlignmentNotification>> result = new ArrayList<KeyValue<String, IntersectionReferenceAlignmentNotification>>();

                IntersectionReferenceAlignmentNotification notification = new IntersectionReferenceAlignmentNotification();
                notification.setEvent(value);
                notification.setNotificationText("Intersection Reference Alignment Notification, generated because corresponding intersection reference alignment event was generated.");
                notification.setNotificationHeading("Intersection Reference Alignment");
                result.add(new KeyValue<>(key, notification));
                return result;
            }
        );

        KTable<String, IntersectionReferenceAlignmentNotification> notificationTable = 
            notificationEventStream.groupByKey(Grouped.with(Serdes.String(), JsonSerdes.IntersectionReferenceAlignmentNotification()))
            .reduce(
                (oldValue, newValue)->{
                        return oldValue;
                },
            Materialized.<String, IntersectionReferenceAlignmentNotification, KeyValueStore<Bytes, byte[]>>as("IntersectionReferenceAlignmentNotification")
            .withKeySerde(Serdes.String())
            .withValueSerde(JsonSerdes.IntersectionReferenceAlignmentNotification())
        );

        notificationTable.toStream().to(
            parameters.getIntersectionReferenceAlignmentNotificationTopicName(),
            Produced.with(Serdes.String(),
                    JsonSerdes.IntersectionReferenceAlignmentNotification()));

        if(parameters.isDebug()){
            notificationTable.toStream().print(Printed.toSysOut());
        }
                    
        return builder.build();
    }    

    @Override
    public void stop() {
        logger.info("Stopping ConnectionOfTravelEventAssessmentTopology.");
        if (streams != null) {
            streams.close();
            streams.cleanUp();
            streams = null;
        }
        logger.info("Stopped ConnectionOfTravelEventAssessmentTopology.");
    }    
}