package com.prdv.rdv.planning.application.port.output;

/** Anti-corruption boundary: planning knows site ownership, not profile entities. */
public interface PracticeSiteAccess { void requireOwned(Long practitioner,String site); }
