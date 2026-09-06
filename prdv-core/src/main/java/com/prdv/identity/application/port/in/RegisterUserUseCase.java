package com.prdv.identity.application.port.in;

import com.prdv.identity.application.model.UserInfo;

/**
 * Port d'entree (DRIVING PORT) : le monde exterieur (REST, CLI, batch) ne peut appeler
 * QUE ce que le coeur expose ici. Patterns PORTS & ADAPTERS + FACADE (le handler).
 */
public interface RegisterUserUseCase {

    record Command(String email, String phone, String password, String role, String inviteCode) { }

    UserInfo register(Command command);
}
