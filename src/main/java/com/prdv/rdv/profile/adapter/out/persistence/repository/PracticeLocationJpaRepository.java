package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PracticeLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeLocationJpaRepository extends JpaRepository<PracticeLocationEntity, String> {

    List<PracticeLocationEntity> findByPractitionerUserIdOrderByMainLocationDesc(Long practitionerUserId);

    List<PracticeLocationEntity> findByCityIgnoreCase(String city);

    void deleteByPractitionerUserId(Long practitionerUserId);
}
