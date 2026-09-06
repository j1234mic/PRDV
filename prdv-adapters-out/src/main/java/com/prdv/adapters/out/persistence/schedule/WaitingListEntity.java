package com.prdv.adapters.out.persistence.schedule;

import com.prdv.schedule.domain.model.WaitingListEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "waiting_list_entries", indexes = @Index(name = "idx_waitlist_doctor", columnList = "doctor_user_id,status"))
public class WaitingListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_user_id", nullable = false)
    private Long patientUserId;

    @Column(name = "doctor_user_id", nullable = false)
    private Long doctorUserId;

    @Column(name = "earliest_date", nullable = false)
    private LocalDate earliest;

    @Column(name = "latest_date", nullable = false)
    private LocalDate latest;

    @Column(nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitingListEntry.State state;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientUserId() { return patientUserId; }
    public void setPatientUserId(Long v) { this.patientUserId = v; }
    public Long getDoctorUserId() { return doctorUserId; }
    public void setDoctorUserId(Long v) { this.doctorUserId = v; }
    public LocalDate getEarliest() { return earliest; }
    public void setEarliest(LocalDate v) { this.earliest = v; }
    public LocalDate getLatest() { return latest; }
    public void setLatest(LocalDate v) { this.latest = v; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public WaitingListEntry.State getState() { return state; }
    public void setState(WaitingListEntry.State state) { this.state = state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
