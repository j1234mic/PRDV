package com.prdv.adapters.out.persistence.profile;

import com.prdv.profile.domain.model.DoctorVerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface DoctorProfileJpaRepository extends JpaRepository<DoctorProfileEntity, Long> {

    Optional<DoctorProfileEntity> findByUserId(Long userId);

    Optional<DoctorProfileEntity> findByRpps(String rpps);

    @Query("""
            select d from DoctorProfileEntity d
            where d.verificationStatus = :verified
              and (:q = '' or lower(d.specialty) like lower(concat('%', :q, '%')))
            order by d.fullName asc, d.id asc
            """)
    List<DoctorProfileEntity> searchVerified(@Param("verified") DoctorVerificationStatus verified,
                                             @Param("q") String query, Pageable pageable);

    List<DoctorProfileEntity> findByVerificationStatusOrderByFullNameAsc(DoctorVerificationStatus status,
                                                                           Pageable pageable);
}
