package com.prdv.adapters.out.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

interface OtpJpaRepository extends JpaRepository<OtpEntity, String> {
}
