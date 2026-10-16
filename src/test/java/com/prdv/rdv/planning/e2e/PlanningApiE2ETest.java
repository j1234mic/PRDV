package com.prdv.rdv.planning.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.prdv.rdv.iam.application.port.output.*;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.*;
import com.prdv.rdv.planning.domain.Agenda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="prdv.planning.waiting-offers.enabled=false")
class PlanningApiE2ETest {
    @Autowired TestRestTemplate rest;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired JwtTokenPort tokens;
    @Autowired Clock clock;
    @Autowired com.prdv.rdv.profile.application.port.output.PracticeLocationRepository locations;
    @Test void realHttpJwtBookingConflictCancellationAndIsolation() {
        User doctor=user(Role.PRACTITIONER,ProfileType.PRACTITIONER);
        User patient=user(Role.PATIENT,ProfileType.PATIENT);
        User stranger=user(Role.PATIENT,ProfileType.PATIENT);
        String doctorToken=token(doctor),patientToken=token(patient),strangerToken=token(stranger);
        String base="/api/v1/planning/";
        assertThat(request(null,HttpMethod.POST,base+"agenda",null).getStatusCode().value()).isEqualTo(401);
        assertThat(request(patientToken,HttpMethod.POST,base+"agenda",null).getStatusCode().value()).isEqualTo(403);
        assertThat(request(doctorToken,HttpMethod.POST,base+"agenda",null).getStatusCode().value()).isEqualTo(201);
        Instant start=clock.instant().plusSeconds(86400);
        var location=locations.save(com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation.create(
                doctor.getId(),"Cabinet","Rue de test","101","Antananarivo","MG",true,clock));
        Agenda.Slot slot=new Agenda.Slot(UUID.randomUUID(),location.getId(),start,start.plusSeconds(1800),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        assertThat(request(doctorToken,HttpMethod.POST,base+"agenda/slots",List.of(slot)).getStatusCode().value()).isEqualTo(200);
        String path=base+doctor.getId();
        Agenda.Intake intake=new Agenda.Intake("Moi","Suivi",null,Map.of(),List.of(),false,"EMAIL");
        Map<String,Object> booking=Map.of("slot",slot.id(),"intake",intake,"firstVisit",false);
        var booked=request(patientToken,HttpMethod.POST,path+"/appointments",booking);
        assertThat(booked.getStatusCode().value()).isEqualTo(200);
        String id=booked.getBody().get("id").asText();
        assertThat(request(strangerToken,HttpMethod.GET,path+"/appointments",null).getBody().size()).isZero();
        var detail=request(patientToken,HttpMethod.GET,path+"/appointments/"+id,null);
        assertThat(detail.getStatusCode().value()).isEqualTo(200);
        assertThat(detail.getBody().get("slot").get("site").asText()).isEqualTo(location.getId());
        assertThat(request(strangerToken,HttpMethod.GET,path+"/appointments/"+id,null).getStatusCode().value()).isEqualTo(403);
        assertThat(request(strangerToken,HttpMethod.POST,path+"/appointments/"+id+"/cancel",Map.of("reason","Test"))
                .getStatusCode().value()).isEqualTo(403);
        assertThat(request(strangerToken,HttpMethod.POST,path+"/appointments",booking).getStatusCode().value()).isEqualTo(409);
        assertThat(request(patientToken,HttpMethod.POST,path+"/appointments/"+id+"/cancel",Map.of("reason","Indisponible"))
                .getStatusCode().value()).isEqualTo(200);
        assertThat(request(strangerToken,HttpMethod.POST,path+"/appointments",booking).getStatusCode().value()).isEqualTo(200);
    }
    @Test void templatesReasonsAutomaticWaitingOfferAndPrivateInboxJourney() {
        User doctor=user(Role.PRACTITIONER,ProfileType.PRACTITIONER);
        User first=user(Role.PATIENT,ProfileType.PATIENT),second=user(Role.PATIENT,ProfileType.PATIENT);
        String doctorToken=token(doctor),firstToken=token(first),secondToken=token(second);
        String root="/api/v1/planning",path=root+"/"+doctor.getId();
        var site=locations.save(com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation.create(
                doctor.getId(),"Cabinet","Rue de test","101","Antananarivo","MG",true,clock));
        assertThat(request(doctorToken,HttpMethod.POST,root+"/agenda",null).getStatusCode().value()).isEqualTo(201);
        var reason=new com.prdv.rdv.planning.domain.ConsultationReason("suivi","Suivi",Agenda.Type.NORMAL,30,Set.of("allergies"),List.of("Ordonnance"));
        assertThat(request(doctorToken,HttpMethod.PUT,root+"/agenda/reasons/suivi",reason).getStatusCode().value()).isEqualTo(200);
        assertThat(request(firstToken,HttpMethod.GET,path+"/reasons",null).getBody().size()).isEqualTo(1);
        LocalDate day=LocalDate.now(clock).plusDays(3);
        var pattern=new com.prdv.rdv.planning.domain.SchedulePattern(day,day,ZoneOffset.UTC,Set.of(day.getDayOfWeek()),null,
                LocalTime.of(9,0),LocalTime.of(9,30),site.getId(),Agenda.Type.NORMAL,Agenda.Audience.ALL,null,1);
        assertThat(request(doctorToken,HttpMethod.PUT,root+"/agenda/templates/matin",pattern).getStatusCode().value()).isEqualTo(200);
        assertThat(request(firstToken,HttpMethod.GET,root+"/agenda/settings",null).getStatusCode().value()).isEqualTo(403);
        assertThat(request(doctorToken,HttpMethod.POST,root+"/agenda/templates/matin/apply",Map.of("from",day,"until",day)).getStatusCode().value()).isEqualTo(200);
        var slots=request(doctorToken,HttpMethod.GET,root+"/agenda/slots",null).getBody();
        String slot=slots.get(0).get("id").asText();
        var intake=new Agenda.Intake("Moi","suivi",null,Map.of("allergies","Aucune"),List.of(),false,"IN_APP");
        var booked=request(firstToken,HttpMethod.POST,path+"/appointments",Map.of("slot",slot,"intake",intake));
        assertThat(booked.getStatusCode().value()).isEqualTo(200);
        var wait=Map.of("from",day.atStartOfDay(ZoneOffset.UTC).toInstant(),"to",day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                "site",site.getId(),"type","NORMAL","urgent",false,"automaticOffer",true);
        assertThat(request(secondToken,HttpMethod.POST,path+"/waiting-list",wait).getStatusCode().value()).isEqualTo(200);
        assertThat(request(firstToken,HttpMethod.POST,path+"/appointments/"+booked.getBody().get("id").asText()+"/cancel",Map.of("reason","Absent"))
                .getStatusCode().value()).isEqualTo(200);
        var offers=request(secondToken,HttpMethod.GET,path+"/waiting-list/offers",null).getBody();
        assertThat(offers.size()).isEqualTo(1);
        String offer=offers.get(0).get("id").asText();
        assertThat(request(firstToken,HttpMethod.GET,path+"/waiting-list/offers",null).getBody().size()).isZero();
        assertThat(request(firstToken,HttpMethod.POST,path+"/waiting-list/offers/"+offer+"/accept",Map.of("intake",intake)).getStatusCode().value()).isEqualTo(404);
        assertThat(request(secondToken,HttpMethod.POST,path+"/waiting-list/offers/"+offer+"/accept",Map.of("intake",intake)).getStatusCode().value()).isEqualTo(200);
        assertThat(request(secondToken,HttpMethod.POST,path+"/waiting-list/offers/"+offer+"/accept",Map.of("intake",intake)).getStatusCode().value()).isEqualTo(404);
        var inbox=request(secondToken,HttpMethod.GET,root+"/notifications",null).getBody();
        assertThat(inbox.size()).isGreaterThanOrEqualTo(2);
        String notification=inbox.get(0).get("id").asText();
        assertThat(request(firstToken,HttpMethod.POST,root+"/notifications/"+notification+"/read",null).getStatusCode().value()).isEqualTo(404);
        assertThat(request(secondToken,HttpMethod.POST,root+"/notifications/"+notification+"/read",null).getStatusCode().value()).isEqualTo(200);
        assertThat(request(doctorToken,HttpMethod.GET,root+"/agenda/audit",null).getBody().size()).isGreaterThan(0);
    }
    private User user(String role,ProfileType type) {
        User u=User.register(Email.of("planning-"+UUID.randomUUID()+"@prdv.test"),null,"hash",type,clock);
        u.markEmailVerified(); u.setStatus(AccountStatus.ACTIVE);
        u.getRoles().add(roles.findByName(role).orElseThrow()); return users.save(u);
    }
    private String token(User u) { return tokens.createAccessToken(u.getId(),u.getEmail(),List.of(),u.getTokenVersion()); }
    private ResponseEntity<JsonNode> request(String token,HttpMethod method,String path,Object body) {
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if(token!=null) headers.setBearerAuth(token);
        return rest.exchange(path,method,new HttpEntity<>(body,headers),JsonNode.class);
    }
}
