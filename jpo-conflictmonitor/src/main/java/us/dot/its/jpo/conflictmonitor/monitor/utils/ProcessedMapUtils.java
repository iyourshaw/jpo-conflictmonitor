package us.dot.its.jpo.conflictmonitor.monitor.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import us.dot.its.jpo.asn.j2735.r2024.MapData.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.BaseFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.*;
import us.dot.its.jpo.ode.plugin.j2735.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Methods to get properties from ProcessedMaps with null checks
 */
@Slf4j
public class ProcessedMapUtils {

    public static <T> long getTimestamp(ProcessedMap<T> processedMap) {
        if (processedMap == null) {
            log.error("ProcessedMap is null");
            return 0L;
        }
        MapSharedProperties properties = processedMap.getProperties();
        if (properties == null) {
            log.error("ProcessedMap.properties are null");
            return 0L;
        }
        ZonedDateTime zdt = properties.getTimeStamp();
        if (zdt == null) {
            log.error("ProcessedMap Timestamp is null");
            return 0L;
        }
        return zdt.toInstant().toEpochMilli();
    }

    public static <T> long getOdeReceivedAt(ProcessedMap<T> processedMap) {
        if (processedMap == null) {
            log.error("ProcessedMap is null");
            return 0L;
        }
        MapSharedProperties properties = processedMap.getProperties();
        if (properties == null) {
            log.error("ProcessedMap.properties are null");
            return 0L;
        }
        ZonedDateTime zdt = properties.getOdeReceivedAt();
        if (zdt == null) {
            log.error("ProcessedMap.OdeReceivedAt is null");
            return 0L;
        }
        return zdt.toInstant().toEpochMilli();
    }

    public static <T> Map<Integer, LaneTypeAttributes> getLaneTypeAttributes(ProcessedMap<T> processedMap) {
        MapFeatureCollection<T> featureCollection = processedMap.getMapFeatureCollection();
        if (featureCollection == null) {
            log.error("ProcessedMap.processedMapFeatureCollection is null");
            return Map.of();
        }
        MapFeature<T>[] features = featureCollection.getFeatures();
        return Arrays.stream(features)
                .map(BaseFeature::getProperties)
                .filter(properties -> properties != null
                        && properties.getLaneId() != null
                        && properties.getLaneType() != null)
                .collect(Collectors.toUnmodifiableMap(MapProperties::getLaneId,
                        props -> oldToNewLaneTypeAttributes(props.getLaneType())));
    }

    /**
     * Convert old to new LaneTypeAttributes.
     * Note that lane types other than 'Vehicle" and 'Crosswalk' only copy over the revocable bit.
     * @param old Old ODE LaneTypeAttributes
     * @return New POJO LaneTypeAttributes
     */
    public static LaneTypeAttributes oldToNewLaneTypeAttributes(J2735LaneTypeAttributes old) {
        LaneTypeAttributes newAttribs = new LaneTypeAttributes();

        J2735BitString bitString;

        if ((bitString = old.getVehicle()) != null) {
            var vehicle = new LaneAttributes_Vehicle();
            vehicle.setIsVehicleRevocableLane(bitString.get(J2735LaneAttributesVehicle.isVehicleRevocableLane.name()));
            vehicle.setIsVehicleFlyOverLane(bitString.get(J2735LaneAttributesVehicle.isVehicleFlyOverLane.name()));
            vehicle.setHovLaneUseOnly(bitString.get(J2735LaneAttributesVehicle.hovLaneUseOnly.name()));
            vehicle.setRestrictedToBusUse(bitString.get(J2735LaneAttributesVehicle.restrictedToBusUse.name()));
            vehicle.setRestrictedToTaxiUse(bitString.get(J2735LaneAttributesVehicle.restrictedToTaxiUse.name()));
            vehicle.setRestrictedFromPublicUse(bitString.get(J2735LaneAttributesVehicle.restrictedFromPublicUse.name()));
            vehicle.setHasIRbeaconCoverage(bitString.get(J2735LaneAttributesVehicle.hasIRbeaconCoverage.name()));
            vehicle.setPermissionOnRequest(bitString.get(J2735LaneAttributesVehicle.permissionOnRequest.name()));
            newAttribs.setVehicle(vehicle);
        } else if ((bitString = old.getCrosswalk()) != null) {
            var crosswalk = new LaneAttributes_Crosswalk();
            crosswalk.setCrosswalkRevocableLane(bitString.get(J2735LaneAttributesCrosswalk.crosswalkRevocableLane.name()));
            crosswalk.setBicyleUseAllowed(bitString.get(J2735LaneAttributesCrosswalk.bicyleUseAllowed.name()));
            crosswalk.setIsXwalkFlyOverLane(bitString.get(J2735LaneAttributesCrosswalk.isXwalkFlyOverLane.name()));
            crosswalk.setFixedCycleTime(bitString.get(J2735LaneAttributesCrosswalk.fixedCycleTime.name()));
            crosswalk.setBiDirectionalCycleTimes(bitString.get(J2735LaneAttributesCrosswalk.biDirectionalCycleTimes.name()));
            crosswalk.setHasPushToWalkButton(bitString.get(J2735LaneAttributesCrosswalk.hasPushToWalkButton.name()));
            crosswalk.setAudioSupport(bitString.get(J2735LaneAttributesCrosswalk.audioSupport.name()));
            crosswalk.setRfSignalRequestPresent(bitString.get(J2735LaneAttributesCrosswalk.rfSignalRequestPresent.name()));
            crosswalk.setUnsignalizedSegmentsPresent(bitString.get(J2735LaneAttributesCrosswalk.unsignalizedSegmentsPresent.name()));
            newAttribs.setCrosswalk(crosswalk);
        } else if ((bitString = old.getMedian()) != null) {
            var barrier = new LaneAttributes_Barrier();
            barrier.setMedian_RevocableLane(bitString.get(J2735LaneAttributesBarrier.medianRevocableLane.name()));
            newAttribs.setMedian(barrier);
        } else if ((bitString = old.getParking()) != null) {
            var parking = new LaneAttributes_Parking();
            parking.setParkingRevocableLane(bitString.get(J2735LaneAttributesParking.parkingRevocableLane.name()));
            newAttribs.setParking(parking);
        } else if ((bitString = old.getSidewalk()) != null) {
            var sidewalk = new LaneAttributes_Sidewalk();
            sidewalk.setSidewalk_RevocableLane(bitString.get(J2735LaneAttributesSidewalk.sidewalkRevocableLane.name()));
            newAttribs.setSidewalk(sidewalk);
        } else if ((bitString = old.getStriping()) != null) {
            var striping = new LaneAttributes_Striping();
            striping.setStripeToConnectingLanesRevocableLane(bitString.get(J2735LaneAttributesStriping.stripeToConnectingLanesRevocableLane.name()));
            newAttribs.setStriping(striping);
        } else if ((bitString = old.getTrackedVehicle()) != null) {
            var trackedVehicle = new LaneAttributes_TrackedVehicle();
            trackedVehicle.setSpec_RevocableLane(bitString.get(J2735LaneAttributesTrackedVehicle.specRevocableLane.name()));
            newAttribs.setTrackedVehicle(trackedVehicle);
        } else if ((bitString = old.getBikeLane()) != null) {
            var bikeLane = new LaneAttributes_Bike();
            bikeLane.setBikeRevocableLane(bitString.get(J2735LaneAttributesBike.bikeRevocableLane.name()));
            newAttribs.setBikeLane(bikeLane);
        }
        return newAttribs;
    }

    public static boolean isRevocableBitSet(LaneTypeAttributes attribs) {
        if (attribs.getBikeLane() != null) {
            return attribs.getBikeLane().isBikeRevocableLane();
        } else if (attribs.getCrosswalk() != null) {
            return attribs.getCrosswalk().isCrosswalkRevocableLane();
        } else if (attribs.getMedian() != null) {
            return attribs.getMedian().isMedian_RevocableLane();
        } else if (attribs.getParking() != null) {
            return attribs.getParking().isParkingRevocableLane();
        } else if (attribs.getSidewalk() != null) {
            return attribs.getSidewalk().isSidewalk_RevocableLane();
        } else if (attribs.getStriping() != null) {
            return attribs.getStriping().isStripeToConnectingLanesRevocableLane();
        } else if (attribs.getTrackedVehicle() != null) {
            return attribs.getTrackedVehicle().isSpec_RevocableLane();
        } else if (attribs.getVehicle() != null) {
            return attribs.getVehicle().isIsVehicleRevocableLane();
        }
        return false;
    }




}
