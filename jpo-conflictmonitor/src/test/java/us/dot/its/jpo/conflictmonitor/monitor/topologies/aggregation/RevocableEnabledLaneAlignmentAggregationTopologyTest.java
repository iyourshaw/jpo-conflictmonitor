package us.dot.its.jpo.conflictmonitor.monitor.topologies.aggregation;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.MapData.LaneAttributes_Vehicle;
import us.dot.its.jpo.asn.j2735.r2024.MapData.LaneTypeAttributes;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.aggregation.revocable_enabled_lane_alignment.RevocableEnabledLaneAlignmentAggregationKey;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.RevocableEnabledLaneAlignmentEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.RevocableEnabledLaneAlignmentEventAggregation;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.ode.plugin.j2735.J2735BitString;
import us.dot.its.jpo.ode.plugin.j2735.J2735LaneAttributesVehicle;
import us.dot.its.jpo.ode.plugin.j2735.J2735LaneTypeAttributes;
import us.dot.its.jpo.ode.plugin.j2735.J2735MovementPhaseState;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;


public class RevocableEnabledLaneAlignmentAggregationTopologyTest
    extends
            BaseAggregationTopologyTest<
                    RsuIntersectionKey,
                    RevocableEnabledLaneAlignmentEvent,
                    RevocableEnabledLaneAlignmentAggregationKey,
                    RevocableEnabledLaneAlignmentEventAggregation,
                    RevocableEnabledLaneAlignmentAggregationTopology>{

    final J2735MovementPhaseState state = J2735MovementPhaseState.PROTECTED_MOVEMENT_ALLOWED;
    final Set<Integer> enabledLanes = Set.of(1, 2, 3);
    final Set<Integer> revocableLanes = Set.of(1, 2);
    final long timestamp = System.currentTimeMillis();

    @Test
    public void testTopology() {
        List<KeyValue<RevocableEnabledLaneAlignmentAggregationKey, RevocableEnabledLaneAlignmentEventAggregation>> resultList
                = runTestTopology();
        assertThat("Should have produced 1 aggregated event", resultList, hasSize(1));
        var result = resultList.getFirst();
        var resultKey = result.key;
        assertThat(resultKey.getRsuId(), equalTo(rsuId));
        assertThat(resultKey.getIntersectionId(), equalTo(intersectionId));
        assertThat(resultKey.getRegion(), equalTo(region));
        assertThat(resultKey.getEventState(), equalTo(state));
        var resultValue = result.value;
        assertThat(resultValue.getNumberOfEvents(), equalTo(numberOfEvents));
        assertThat(resultValue.getIntersectionID(), equalTo(intersectionId));
        assertThat(resultValue.getRoadRegulatorID(), equalTo(region));
    }

    @Override
    String outputTopicName() {
        return "topic.CmRevocableEnabledLaneAlignmentEventAggregation";
    }

    @Override
    Serde<RsuIntersectionKey> eventKeySerde() {
        return us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes.RsuIntersectionKey();
    }

    @Override
    Serde<RevocableEnabledLaneAlignmentEvent> eventSerde() {
        return JsonSerdes.RevocableEnabledLaneAlignmentEvent();
    }

    @Override
    Serde<RevocableEnabledLaneAlignmentAggregationKey> aggKeySerde() {
        return JsonSerdes.RevocableEnabledLaneAlignmentAggregationKey();
    }

    @Override
    Serde<RevocableEnabledLaneAlignmentEventAggregation> aggEventSerde() {
        return JsonSerdes.RevocableEnabledLaneAlignmentEventAggregation();
    }

    @Override
    RsuIntersectionKey createKey() {
        return new RsuIntersectionKey();
    }

    @Override
    RevocableEnabledLaneAlignmentEvent createEvent() {
        var event = new RevocableEnabledLaneAlignmentEvent();
        event.setSource(rsuId);
        event.setIntersectionID(intersectionId);
        event.setRoadRegulatorID(region);
        event.setEventState(state);
        event.setEnabledLaneList(enabledLanes);
        event.setRevocableLaneList(revocableLanes);
        event.setTimestamp(timestamp);
        event.setLaneTypeAttributes(getLaneTypeAttributes());
        return event;
    }

    @Override
    RevocableEnabledLaneAlignmentAggregationTopology createTopology() {
        return new RevocableEnabledLaneAlignmentAggregationTopology();
    }

    @Override
    KStream<RevocableEnabledLaneAlignmentAggregationKey, RevocableEnabledLaneAlignmentEvent>
    selectAggKey(KStream<RsuIntersectionKey, RevocableEnabledLaneAlignmentEvent> instream) {
        return instream.selectKey((key, value) -> {
            var newKey = new RevocableEnabledLaneAlignmentAggregationKey();
            newKey.setIntersectionId(key.getIntersectionId());
            newKey.setRegion(key.getRegion());
            newKey.setRsuId(key.getRsuId());
            newKey.setEventState(value.getEventState());
            return newKey;
        });
    }

    private Map<Integer, LaneTypeAttributes> getLaneTypeAttributes() {

        LaneAttributes_Vehicle bs = new LaneAttributes_Vehicle();

        LaneAttributes_Vehicle bsRevocable = new LaneAttributes_Vehicle();
        bsRevocable.setIsVehicleRevocableLane(true);

        var attrib1 = new LaneTypeAttributes();
        var attrib2 = new LaneTypeAttributes();
        var attrib3 = new LaneTypeAttributes();
        var attrib4 = new LaneTypeAttributes();
        attrib1.setVehicle(bsRevocable);
        attrib2.setVehicle(bsRevocable);
        attrib3.setVehicle(bs);
        attrib4.setVehicle(bs);

        return Map.of(1, attrib1, 2, attrib2, 3, attrib3, 4, attrib4);
    }
}
