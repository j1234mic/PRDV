package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/** Consultation et revocation des sessions / appareils connectes. */
public interface SessionUseCase {

    List<Views.SessionView> listMySessions();

    void revoke(Long sessionId);

    /** Deconnexion de toutes les sessions (et revocation des refresh tokens). */
    void revokeAll();
}
