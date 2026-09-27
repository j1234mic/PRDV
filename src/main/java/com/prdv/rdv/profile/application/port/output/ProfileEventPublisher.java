package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.event.ProfileEvent;

/** Publication des evenements du contexte profils (pattern Observer). */
public interface ProfileEventPublisher {

    void publish(ProfileEvent event);
}
