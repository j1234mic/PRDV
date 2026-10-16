package com.prdv.rdv.planning.adapter.out.profile;

import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.planning.domain.PlanningException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfilePracticeSiteAccessTest {
    @Test void rejectsUnknownAndForeignSites() {
        var repository=mock(PracticeLocationRepository.class);
        var access=new ProfilePracticeSiteAccess(repository);
        var site=PracticeLocation.create(1L,"Cabinet","Rue","101","Antananarivo","MG",true,Clock.systemUTC());
        when(repository.findById("unknown")).thenReturn(Optional.empty());
        when(repository.findById(site.getId())).thenReturn(Optional.of(site));
        assertThatCode(() -> access.requireOwned(1L,site.getId())).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireOwned(2L,site.getId())).isInstanceOf(PlanningException.class).hasMessageContaining("appartient");
        assertThatThrownBy(() -> access.requireOwned(1L,"unknown")).isInstanceOf(PlanningException.class).hasMessageContaining("inconnu");
    }
}
