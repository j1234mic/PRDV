package com.prdv.rdv.planning.application.port.input;

/** System-only orchestration, never exposed as a patient mutation endpoint. */
public interface PlanningMaintenanceUseCase { int reconcileWaitingOffers(); }
