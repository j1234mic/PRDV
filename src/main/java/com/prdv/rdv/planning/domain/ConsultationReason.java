package com.prdv.rdv.planning.domain;

import java.util.*;
import static com.prdv.rdv.planning.domain.PlanningException.*;

/** Public, non-clinical booking instructions owned by the practitioner. */
public record ConsultationReason(String id, String label, Agenda.Type type, int minutes,
                                 Set<String> requiredQuestions, List<String> documents) {
    public ConsultationReason {
        require(id != null && id.matches("[a-zA-Z0-9_-]{1,80}") && label != null && !label.isBlank()
                && label.length() <= 200 && type != null && type.bookable() && minutes > 0 && minutes <= 480,
                Code.INVALID,"Motif de consultation invalide");
        requiredQuestions=Set.copyOf(requiredQuestions); documents=List.copyOf(documents);
        require(requiredQuestions.size()<=50 && documents.size()<=30,Code.INVALID,"Instructions trop volumineuses");
    }
    public void validate(Agenda.Slot slot, Agenda.Intake intake) {
        require(slot.type()==type && java.time.Duration.between(slot.start(),slot.end()).toMinutes()>=minutes,
                Code.INVALID,"Creneau incompatible avec le motif");
        require(requiredQuestions.stream().allMatch(q -> intake.questionnaire().containsKey(q)
                && !intake.questionnaire().get(q).isBlank()),Code.INVALID,"Questionnaire incomplet");
    }
}
