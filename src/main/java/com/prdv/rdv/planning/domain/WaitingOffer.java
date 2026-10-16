package com.prdv.rdv.planning.domain;

import java.time.Instant;
import java.util.UUID;

/** A temporary capacity hold, never an appointment until accepted by its patient. */
public record WaitingOffer(UUID id, UUID waitingId, UUID slotId, Long patientId, Instant expiresAt) {}
