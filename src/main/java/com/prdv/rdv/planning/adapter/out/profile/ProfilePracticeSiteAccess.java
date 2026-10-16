package com.prdv.rdv.planning.adapter.out.profile;

import com.prdv.rdv.planning.application.port.output.PracticeSiteAccess;
import com.prdv.rdv.planning.domain.PlanningException;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfilePracticeSiteAccess implements PracticeSiteAccess {
    private final PracticeLocationRepository locations;
    public ProfilePracticeSiteAccess(PracticeLocationRepository locations) { this.locations=locations; }
    public void requireOwned(Long practitioner,String site) {
        var location=locations.findById(site).orElseThrow(() -> new PlanningException(PlanningException.Code.INVALID,"Lieu d'exercice inconnu"));
        PlanningException.require(practitioner.equals(location.getPractitionerUserId()),PlanningException.Code.FORBIDDEN,
                "Ce lieu n'appartient pas au praticien");
    }
}
