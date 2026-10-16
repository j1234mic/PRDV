package com.prdv.rdv.planning.application.port.output;

public interface PlanningActor {
    Long userId();
    default java.util.Optional<Long> currentUserId() { return java.util.Optional.ofNullable(userId()); }
}
