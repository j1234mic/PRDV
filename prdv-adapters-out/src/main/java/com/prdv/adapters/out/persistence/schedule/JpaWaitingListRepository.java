package com.prdv.adapters.out.persistence.schedule;

import com.prdv.schedule.application.port.out.WaitingListRepository;
import com.prdv.schedule.domain.model.WaitingListEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaWaitingListRepository implements WaitingListRepository {

    private final WaitingListJpaRepository jpa;

    public JpaWaitingListRepository(WaitingListJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public WaitingListEntry save(WaitingListEntry entry) {
        WaitingListEntity e = new WaitingListEntity();
        e.setId(entry.id());
        e.setPatientUserId(entry.patientUserId());
        e.setDoctorUserId(entry.doctorUserId());
        e.setEarliest(entry.earliest());
        e.setLatest(entry.latest());
        e.setUrgent(entry.urgent());
        e.setState(entry.state());
        e.setCreatedAt(entry.createdAt());
        return new WaitingListEntry(e.getId() != null ? e.getId() : entry.id(), entry.patientUserId(),
                entry.doctorUserId(), entry.earliest(), entry.latest(), entry.urgent(), entry.state(),
                entry.createdAt());
    }

    @Override
    public List<WaitingListEntry> findOpenByDoctor(Long doctorUserId) {
        return jpa.findByDoctorUserIdAndStateOrderByCreatedAtAsc(doctorUserId, WaitingListEntry.State.OPEN).stream()
                .map(e -> new WaitingListEntry(e.getId(), e.getPatientUserId(), e.getDoctorUserId(),
                        e.getEarliest(), e.getLatest(), e.isUrgent(), e.getState(), e.getCreatedAt()))
                .toList();
    }

    @Override
    public void deleteByPatientAndDoctor(Long patientUserId, Long doctorUserId) {
        jpa.deleteOpenEntries(patientUserId, doctorUserId, WaitingListEntry.State.OPEN);
    }
}
