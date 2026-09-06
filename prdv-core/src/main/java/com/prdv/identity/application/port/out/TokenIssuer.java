package com.prdv.identity.application.port.out;

import com.prdv.identity.application.model.AuthTokens;
import com.prdv.identity.domain.model.User;

/** Port de sortie : emission des jetons (adaptateur JWT dans prdv-adapters-out). */
public interface TokenIssuer {
    AuthTokens issue(User user);
}
