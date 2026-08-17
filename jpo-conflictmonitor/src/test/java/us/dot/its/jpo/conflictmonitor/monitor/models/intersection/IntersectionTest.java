package us.dot.its.jpo.conflictmonitor.monitor.models.intersection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import us.dot.its.jpo.conflictmonitor.monitor.models.Intersection.Intersection;
import us.dot.its.jpo.conflictmonitor.monitor.models.Intersection.LaneConnection;
import us.dot.its.jpo.conflictmonitor.testutils.ResourceUtils;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IntersectionTest {

    private record Expected(int ingress, int egress, int signalGroup, int connectionId) {}

    private static final Expected[] EXPECTED_CONNECTIONS = new Expected[]{
            new Expected(22, 21, 208, 0),
            new Expected(22, 15, 202, 1),
            new Expected(2, 7, 3, 2),
            new Expected(16, 15, 202, 3),
            new Expected(16, 17, 204, 4),
            new Expected(1, 14, 18, 5),
            new Expected(1, 10, 8, 6),
            new Expected(9, 14, 7, 7),
            new Expected(12, 7, 6, 8),
            new Expected(11, 10, 16, 9),
            new Expected(13, 3, 1, 10),
            new Expected(20, 19, 206, 11),
            new Expected(20, 21, 208, 12),
            new Expected(4, 3, 12, 13),
            new Expected(5, 14, 2, 14),
            new Expected(6, 10, 5, 15),
            new Expected(8, 3, 4, 16),
            new Expected(8, 7, 14, 17),
            new Expected(18, 17, 204, 18),
            new Expected(18, 19, 206, 19),
    };

    @SuppressWarnings("unchecked")
    private static ProcessedMap<LineString> getProcessedMap() throws Exception {
        String mapStr = ResourceUtils.loadResource(
                "/us/dot/its/jpo/conflictmonitor/monitor/models/Intersection/ProcessedMap_5187.json");
        ObjectMapper mapper = DateJsonMapper.getInstance();
        return (ProcessedMap<LineString>) mapper.readValue(mapStr, ProcessedMap.class);
    }

    private static LaneConnection findConnection(Intersection intersection, int ingressId, int egressId) {
        for (LaneConnection connection : intersection.getLaneConnections()) {
            if (connection.getIngressLane() != null && connection.getEgressLane() != null
                    && connection.getIngressLane().getId() == ingressId
                    && connection.getEgressLane().getId() == egressId) {
                return connection;
            }
        }
        return null;
    }

    @Test
    public void testFromProcessedMap_laneConnections() throws Exception {
        var map = getProcessedMap();
        var intersection = Intersection.fromProcessedMap(map);

        assertThat(intersection.getLaneConnections(), hasSize(EXPECTED_CONNECTIONS.length));

        for (Expected expected : EXPECTED_CONNECTIONS) {
            String description = expected.ingress() + "->" + expected.egress();
            LaneConnection connection = findConnection(intersection, expected.ingress(), expected.egress());
            assertThat(description, connection, notNullValue());
            assertThat(description, connection.getSignalGroup(), equalTo(expected.signalGroup()));
            assertThat(description, connection.getConnectionId(), equalTo(expected.connectionId()));
        }
    }
}
