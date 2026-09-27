package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.HealthAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthAlertJpaRepository extends JpaRepository<HealthAlertEntity, String> {

    List<HealthAlertEntity> findByUserIdOrderByTriggeredAtDesc(Long userId);

    List<HealthAlertEntity> findByUserIdAndAcknowledgedFalseOrderByTriggeredAtDesc(Long userId);

    void deleteByUserId(Long userId);
}
