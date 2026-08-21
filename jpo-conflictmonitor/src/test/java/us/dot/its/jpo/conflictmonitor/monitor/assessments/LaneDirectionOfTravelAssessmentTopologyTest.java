package us.dot.its.jpo.conflictmonitor.monitor.assessments;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.Test;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.lane_direction_of_travel_assessment.LaneDirectionOfTravelAssessmentParameters;
import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.LaneDirectionOfTravelAssessment;
import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.LaneDirectionOfTravelAssessmentGroup;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.LaneDirectionOfTravelNotification;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.JsonSerdes;
import us.dot.its.jpo.conflictmonitor.monitor.topologies.assessments.LaneDirectionOfTravelAssessmentTopology;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;


public class LaneDirectionOfTravelAssessmentTopologyTest {
    String kafkaTopicLaneDirectionOfTravelEvent = "topic.CmLaneDirectionOfTravelEvent";
    String kafkaTopicLaneDirectionOfTravelAssessment = "topic.CmLaneDirectionOfTravelAssessment";
    String laneDirectionOfTravelEventKey = "12109";
    String laneDirectionOfTravelEvent = "{\"eventGeneratedAt\":1673394387458,\"eventType\":\"LaneDirectionOfTravel\",\"timestamp\":1655493252811,\"roadRegulatorID\":0,\"intersectionID\":12109,\"laneID\":12,\"laneSegmentNumber\":8,\"laneSegmentInitialLatitude\":39.58972728935065,\"laneSegmentInitialLongitude\":-105.091329041372,\"laneSegmentFinalLatitude\":39.59003379187557,\"laneSegmentFinalLongitude\":-105.09136780827767,\"expectedHeading\":100.25,\"medianVehicleHeading\":174.25,\"medianDistanceFromCenterline\":96.50633375287359,\"aggregateBSMCount\":12}";
    String laneDirectionOfTravelHeadingNotificationEvent = "{\"eventGeneratedAt\":1673394387458,\"eventType\":\"LaneDirectionOfTravel\",\"timestamp\":1655493252811,\"roadRegulatorID\":0,\"intersectionID\":12109,\"laneID\":12,\"laneSegmentNumber\":8,\"laneSegmentInitialLatitude\":39.58972728935065,\"laneSegmentInitialLongitude\":-105.091329041372,\"laneSegmentFinalLatitude\":39.59003379187557,\"laneSegmentFinalLongitude\":-105.09136780827767,\"expectedHeading\":100.25,\"medianVehicleHeading\":174.25,\"medianDistanceFromCenterline\":25,\"aggregateBSMCount\":12}";
    String laneDirectionOfTravelCenterlineNotificationEvent = "{\"eventGeneratedAt\":1673394387458,\"eventType\":\"LaneDirectionOfTravel\",\"timestamp\":1655493252811,\"roadRegulatorID\":0,\"intersectionID\":12109,\"laneID\":12,\"laneSegmentNumber\":8,\"laneSegmentInitialLatitude\":39.58972728935065,\"laneSegmentInitialLongitude\":-105.091329041372,\"laneSegmentFinalLatitude\":39.59003379187557,\"laneSegmentFinalLongitude\":-105.09136780827767,\"expectedHeading\":174.25,\"medianVehicleHeading\":174.25,\"medianDistanceFromCenterline\":96.50633375287359,\"aggregateBSMCount\":12}";
    String laneDirectionOfTravelAssessmentNotificationOutputTopicName = "topic.CmLaneDirectionOfTravelNotification";


    @Test
    public void testAssessment() {
        LaneDirectionOfTravelAssessmentTopology assessment = new LaneDirectionOfTravelAssessmentTopology();
        LaneDirectionOfTravelAssessmentParameters parameters = new LaneDirectionOfTravelAssessmentParameters();
        parameters.setDebug(false);
        parameters.setHeadingToleranceDegrees(20);
        parameters.setLaneDirectionOfTravelEventTopicName(kafkaTopicLaneDirectionOfTravelEvent);
        parameters.setLaneDirectionOfTravelAssessmentOutputTopicName(kafkaTopicLaneDirectionOfTravelAssessment);
        parameters.setLookBackPeriodDays(60);
        parameters.setLookBackPeriodGraceTimeSeconds(30);
        parameters.setLaneDirectionOfTravelNotificationOutputTopicName(laneDirectionOfTravelAssessmentNotificationOutputTopicName);
        parameters.setMinimumNumberOfEvents(1);
        parameters.setDistanceFromCenterlineToleranceCm(50);
        assessment.setParameters(parameters);


        Topology topology = assessment.buildTopology();

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<String, String> inputTopic = driver.createInputTopic(
                kafkaTopicLaneDirectionOfTravelEvent, 
                Serdes.String().serializer(), 
                Serdes.String().serializer());


            TestOutputTopic<String, LaneDirectionOfTravelAssessment> outputTopic = driver.createOutputTopic(
                kafkaTopicLaneDirectionOfTravelAssessment, 
                Serdes.String().deserializer(), 
                JsonSerdes.LaneDirectionOfTravelAssessment().deserializer());
            
            inputTopic.pipeInput(laneDirectionOfTravelEventKey, laneDirectionOfTravelEvent);

            List<KeyValue<String, LaneDirectionOfTravelAssessment>> assessmentResults = outputTopic.readKeyValuesToList();
            
            assertEquals(assessmentResults.size(),1);

            LaneDirectionOfTravelAssessment output = assessmentResults.get(0).value;
            
            assertEquals(output.getRoadRegulatorID(), 0);
            assertEquals(output.getIntersectionID(), 12109);
            
            List<LaneDirectionOfTravelAssessmentGroup> groups = output.getLaneDirectionOfTravelAssessmentGroup();
            assertEquals(groups.size(), 1);
            
            LaneDirectionOfTravelAssessmentGroup group = groups.get(0);
            assertEquals(group.getLaneID(), 12);
            assertEquals(group.getSegmentID(), 8);
            assertEquals(group.getInToleranceEvents(), 0);
            assertEquals(group.getOutOfToleranceEvents(), 1);
            assertEquals(group.getMedianInToleranceHeading(), 0);
            assertEquals(group.getMedianInToleranceCenterlineDistance(), 0);
            assertEquals(group.getMedianHeading(), 174.25);
            assertEquals(group.getMedianCenterlineDistance(), 96.50633375287359);
            assertEquals(group.getTolerance(), 20);

           
        }
    }

    @Test
    public void testNotification() {
        LaneDirectionOfTravelAssessmentTopology assessment = new LaneDirectionOfTravelAssessmentTopology();
        LaneDirectionOfTravelAssessmentParameters parameters = new LaneDirectionOfTravelAssessmentParameters();
        parameters.setDebug(false);
        parameters.setHeadingToleranceDegrees(20);
        parameters.setLaneDirectionOfTravelEventTopicName(kafkaTopicLaneDirectionOfTravelEvent);
        parameters.setLaneDirectionOfTravelAssessmentOutputTopicName(kafkaTopicLaneDirectionOfTravelAssessment);
        parameters.setLookBackPeriodDays(60);
        parameters.setLookBackPeriodGraceTimeSeconds(30);
        parameters.setLaneDirectionOfTravelAssessmentOutputTopicName(kafkaTopicLaneDirectionOfTravelAssessment);
        parameters.setLaneDirectionOfTravelNotificationOutputTopicName(laneDirectionOfTravelAssessmentNotificationOutputTopicName);
        parameters.setMinimumNumberOfEvents(1);
        parameters.setDistanceFromCenterlineToleranceCm(50);
        assessment.setParameters(parameters);


        Topology topology = assessment.buildTopology();

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<String, String> inputTopic = driver.createInputTopic(
                kafkaTopicLaneDirectionOfTravelEvent, 
                Serdes.String().serializer(), 
                Serdes.String().serializer());

            TestOutputTopic<String, LaneDirectionOfTravelNotification> notificationOutputTopic = driver.createOutputTopic(
                laneDirectionOfTravelAssessmentNotificationOutputTopicName, 
                Serdes.String().deserializer(), 
                JsonSerdes.LaneDirectionOfTravelAssessmentNotification().deserializer());
            
            inputTopic.pipeInput(laneDirectionOfTravelEventKey, laneDirectionOfTravelHeadingNotificationEvent);

            List<KeyValue<String, LaneDirectionOfTravelNotification>> notificationResults = notificationOutputTopic.readKeyValuesToList();
            
            assertEquals(notificationResults.size(),1);

            LaneDirectionOfTravelNotification output = notificationResults.get(0).value;
            
            assertEquals(output.getNotificationHeading(), "Lane Direction of Travel Assessment");
            assertEquals(output.getNotificationText(), "Lane Direction of Travel Assessment Notification. The median heading: 174 degrees for segment 8 of lane 12 is not within the allowed tolerance 20.0 degrees of the expected heading 100 degrees.");
            assertEquals(output.getNotificationType(), "LaneDirectionOfTravelAssessmentNotification");

            LaneDirectionOfTravelAssessment outputAssessment = output.getAssessment();
            
            assertEquals(outputAssessment.getRoadRegulatorID(), 0);
            assertEquals(outputAssessment.getIntersectionID(), 12109);
            
            List<LaneDirectionOfTravelAssessmentGroup> groups = outputAssessment.getLaneDirectionOfTravelAssessmentGroup();
            assertEquals(groups.size(), 1);
            
            LaneDirectionOfTravelAssessmentGroup group = groups.get(0);
            assertEquals(group.getLaneID(), 12);
            assertEquals(group.getSegmentID(), 8);
            assertEquals(group.getInToleranceEvents(), 0);
            assertEquals(group.getOutOfToleranceEvents(), 1);
            assertEquals(group.getMedianInToleranceHeading(), 0);
            assertEquals(group.getMedianInToleranceCenterlineDistance(), 0);
            assertEquals(group.getMedianHeading(), 174.25);
            assertEquals(group.getMedianCenterlineDistance(), 25);
            assertEquals(group.getTolerance(), 20);

        }
    }

    @Test
    public void testCenterlineDistanceNotification() {
        LaneDirectionOfTravelAssessmentTopology assessment = new LaneDirectionOfTravelAssessmentTopology();
        LaneDirectionOfTravelAssessmentParameters parameters = new LaneDirectionOfTravelAssessmentParameters();
        parameters.setDebug(false);
        parameters.setHeadingToleranceDegrees(20);
        parameters.setLaneDirectionOfTravelEventTopicName(kafkaTopicLaneDirectionOfTravelEvent);
        parameters.setLaneDirectionOfTravelAssessmentOutputTopicName(kafkaTopicLaneDirectionOfTravelAssessment);
        parameters.setLookBackPeriodDays(60);
        parameters.setLookBackPeriodGraceTimeSeconds(30);
        parameters.setLaneDirectionOfTravelAssessmentOutputTopicName(kafkaTopicLaneDirectionOfTravelAssessment);
        parameters.setLaneDirectionOfTravelNotificationOutputTopicName(laneDirectionOfTravelAssessmentNotificationOutputTopicName);
        parameters.setMinimumNumberOfEvents(1);
        parameters.setDistanceFromCenterlineToleranceCm(50);
        assessment.setParameters(parameters);


        Topology topology = assessment.buildTopology();

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<String, String> inputTopic = driver.createInputTopic(
                kafkaTopicLaneDirectionOfTravelEvent, 
                Serdes.String().serializer(), 
                Serdes.String().serializer());

            TestOutputTopic<String, LaneDirectionOfTravelNotification> notificationOutputTopic = driver.createOutputTopic(
                laneDirectionOfTravelAssessmentNotificationOutputTopicName, 
                Serdes.String().deserializer(), 
                JsonSerdes.LaneDirectionOfTravelAssessmentNotification().deserializer());
            
            inputTopic.pipeInput(laneDirectionOfTravelEventKey, laneDirectionOfTravelCenterlineNotificationEvent);

            List<KeyValue<String, LaneDirectionOfTravelNotification>> notificationResults = notificationOutputTopic.readKeyValuesToList();
            
            assertEquals(notificationResults.size(),1);

            LaneDirectionOfTravelNotification output = notificationResults.get(0).value;
            
            assertEquals(output.getNotificationHeading(), "Lane Direction of Travel Assessment");
            assertEquals(output.getNotificationText(), "Lane Direction of Travel Assessment Notification. The median distance from centerline: 97 cm for segment 8 of lane 12 is not within the allowed tolerance 50.0 cm of the center of the lane.");
            assertEquals(output.getNotificationType(), "LaneDirectionOfTravelAssessmentNotification");

            LaneDirectionOfTravelAssessment outputAssessment = output.getAssessment();
            
            assertEquals(outputAssessment.getRoadRegulatorID(), 0);
            assertEquals(outputAssessment.getIntersectionID(), 12109);
            
            List<LaneDirectionOfTravelAssessmentGroup> groups = outputAssessment.getLaneDirectionOfTravelAssessmentGroup();
            assertEquals(groups.size(), 1);
            
            LaneDirectionOfTravelAssessmentGroup group = groups.get(0);
            assertEquals(group.getLaneID(), 12);
            assertEquals(group.getSegmentID(), 8);
            assertEquals(group.getInToleranceEvents(), 1);
            assertEquals(group.getOutOfToleranceEvents(), 0);
            assertEquals(group.getMedianInToleranceHeading(), 174.25);
            assertEquals(group.getMedianInToleranceCenterlineDistance(), 96.50633375287359);
            assertEquals(group.getMedianHeading(), 174.25);
            assertEquals(group.getMedianCenterlineDistance(), 96.50633375287359);
            assertEquals(group.getTolerance(), 20);

        }
    }

    @Test
    public void testWraparoundHeadingDoesNotProduceFalsePositive() {
        // Regression test for a false-positive Lane Direction of Travel notification bug:
        // vehicle headings that straddle the 0/360 degree boundary (e.g. a northbound lane)
        // but are all actually close to the expected road heading of ~2 degrees. Before the
        // fix, the naive numeric median used for medianVehicleHeading/medianHeading sorted
        // these values to [1, 2, 5, 355, 358, 359] and averaged the two middle values,
        // producing a median ~180 degrees away from the true heading and tripping a false
        // heading violation.
        LaneDirectionOfTravelAssessmentTopology assessment = new LaneDirectionOfTravelAssessmentTopology();
        LaneDirectionOfTravelAssessmentParameters parameters = new LaneDirectionOfTravelAssessmentParameters();
        parameters.setDebug(false);
        parameters.setHeadingToleranceDegrees(20);
        parameters.setLaneDirectionOfTravelEventTopicName(kafkaTopicLaneDirectionOfTravelEvent);
        parameters.setLaneDirectionOfTravelAssessmentOutputTopicName(kafkaTopicLaneDirectionOfTravelAssessment);
        parameters.setLookBackPeriodDays(60);
        parameters.setLookBackPeriodGraceTimeSeconds(30);
        parameters.setLaneDirectionOfTravelNotificationOutputTopicName(laneDirectionOfTravelAssessmentNotificationOutputTopicName);
        parameters.setMinimumNumberOfEvents(1);
        parameters.setDistanceFromCenterlineToleranceCm(50);
        assessment.setParameters(parameters);

        Topology topology = assessment.buildTopology();

        double[] wraparoundHeadings = {355.0, 358.0, 2.0, 5.0, 359.0, 1.0};

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<String, String> inputTopic = driver.createInputTopic(
                kafkaTopicLaneDirectionOfTravelEvent,
                Serdes.String().serializer(),
                Serdes.String().serializer());

            TestOutputTopic<String, LaneDirectionOfTravelAssessment> outputTopic = driver.createOutputTopic(
                kafkaTopicLaneDirectionOfTravelAssessment,
                Serdes.String().deserializer(),
                JsonSerdes.LaneDirectionOfTravelAssessment().deserializer());

            for (double heading : wraparoundHeadings) {
                String event = "{\"eventGeneratedAt\":1673394387458,\"eventType\":\"LaneDirectionOfTravel\",\"timestamp\":1655493252811,"
                    + "\"roadRegulatorID\":0,\"intersectionID\":12109,\"laneID\":12,\"laneSegmentNumber\":8,"
                    + "\"laneSegmentInitialLatitude\":39.58972728935065,\"laneSegmentInitialLongitude\":-105.091329041372,"
                    + "\"laneSegmentFinalLatitude\":39.59003379187557,\"laneSegmentFinalLongitude\":-105.09136780827767,"
                    + "\"expectedHeading\":2.0,\"medianVehicleHeading\":" + heading
                    + ",\"medianDistanceFromCenterline\":0,\"aggregateBSMCount\":1}";
                inputTopic.pipeInput(laneDirectionOfTravelEventKey, event);
            }

            List<KeyValue<String, LaneDirectionOfTravelAssessment>> assessmentResults = outputTopic.readKeyValuesToList();
            LaneDirectionOfTravelAssessment output = assessmentResults.get(assessmentResults.size() - 1).value;

            List<LaneDirectionOfTravelAssessmentGroup> groups = output.getLaneDirectionOfTravelAssessmentGroup();
            assertEquals(groups.size(), 1);

            LaneDirectionOfTravelAssessmentGroup group = groups.get(0);
            assertEquals(group.getLaneID(), 12);
            assertEquals(group.getSegmentID(), 8);

            // The circular median lands near the true ~0/360 degree cluster, not the
            // ~180 degree value a naive numeric median would produce.
            assertEquals(0.0, group.getMedianHeading(), 0.01);

            // With the correct median, all six samples are within tolerance of the
            // expected ~2 degree road heading, so no false-positive violation is raised.
            assertEquals(group.getInToleranceEvents(), 6);
            assertEquals(group.getOutOfToleranceEvents(), 0);
            assertFalse(LaneDirectionOfTravelAssessmentTopology.headingViolation(group));
        }
    }
}