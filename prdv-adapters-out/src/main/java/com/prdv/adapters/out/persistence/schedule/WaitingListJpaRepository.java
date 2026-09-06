package com.prdv.adapters.out.persistence.schedule;

import com.prdv.schedule.domain.model.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface WaitingListJpaRepository extends JpaRepository<WaitingListEntity, Long> {

    List<WaitingListEntity> findByDoctorUserIdAndStateOrderByCreatedAtAsc(Long doctorUserId,
                                                                          WaitingListEntry.State state);

    @Modifying
    @Query("""
            delete from WaitingListEntity w
            where w.patientUserId = :patient and w.doctorUserId = :doctor and w.state = :open
            """)
    void deleteOpenEntries(@Param("patient") Long patientUserId, @Param("doctor") Long doctorUserId,
                           @Param("open") WaitingListEntry.State open);
}
