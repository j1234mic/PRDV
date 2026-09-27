package com.prdv.rdv.profile.adapter.out.event;

import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapteur evenementiel : relaie les evenements du contexte profils sur le
 * bus Spring (pattern Observer).
 */
@Component
public class SpringProfileEventPublisher implements ProfileEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringProfileEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(ProfileEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
