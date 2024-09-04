package us.dot.its.jpo.conflictmonitor.monitor.models.events.spat_transition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.Event;
import us.dot.its.jpo.conflictmonitor.monitor.models.spat_transition.PhaseStateTransition;
import us.dot.its.jpo.conflictmonitor.monitor.models.spat_transition.SpatMovementEventTransition;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class IllegalSpatTransitionEvent extends Event {

    public IllegalSpatTransitionEvent() {
        super("IllegalSpatTransition");
    }

    String source;

    SpatMovementEventTransition transition;

}
