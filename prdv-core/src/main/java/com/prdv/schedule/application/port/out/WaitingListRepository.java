package com.prdv.schedule.application.port.out;

import com.prdv.schedule.domain.model.WaitingListEntry;

import java.util.List;

public interface WaitingListRepository {
    WaitingListEntry save(WaitingListEntry entry);
    List<WaitingListEntry> findOpenByDoctor(Long doctorUserId);
    void deleteByPatientAndDoctor(Long patientUserId, Long doctorUserId);
}
