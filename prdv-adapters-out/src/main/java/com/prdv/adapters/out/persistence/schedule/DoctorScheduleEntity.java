package com.prdv.adapters.out.persistence.schedule;

import com.prdv.adapters.out.persistence.json.ScheduleExceptionListConverter;
import com.prdv.adapters.out.persistence.json.WeeklyAvailabilityRuleListConverter;
import com.prdv.schedule.domain.model.ScheduleException;
import com.prdv.schedule.domain.model.WeeklyAvailabilityRule;
import com.prdv.schedule.domain.policy.CancellationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;

/** L'agenda est un AGREGAT : ses regles vivent en JSON, rechargees en bloc (pas de N+1). */
@Entity
@Table(name = "doctor_schedules")
public class DoctorScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_user_id", nullable = false, unique = true)
    private Long doctorUserId;

    @Convert(converter = WeeklyAvailabilityRuleListConverter.class)
    @Column(name = "weekly_rules", nullable = false, columnDefinition = "TEXT")
    private List<WeeklyAvailabilityRule> weeklyRules;

    @Convert(converter = ScheduleExceptionListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ScheduleException> exceptions;

    @Column(name = "min_notice_hours", nullable = false)
    private int minNoticeHours;

    @Column(name = "max_horizon_days", nullable = false)
    private int maxHorizonDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_mode", nullable = false, length = 20)
    private CancellationMode cancellationMode;

    @Column(name = "requires_confirmation", nullable = false)
    private boolean requiresConfirmation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDoctorUserId() { return doctorUserId; }
    public void setDoctorUserId(Long doctorUserId) { this.doctorUserId = doctorUserId; }
    public List<WeeklyAvailabilityRule> getWeeklyRules() { return weeklyRules; }
    public void setWeeklyRules(List<WeeklyAvailabilityRule> weeklyRules) { this.weeklyRules = weeklyRules; }
    public List<ScheduleException> getExceptions() { return exceptions; }
    public void setExceptions(List<ScheduleException> exceptions) { this.exceptions = exceptions; }
    public int getMinNoticeHours() { return minNoticeHours; }
    public void setMinNoticeHours(int minNoticeHours) { this.minNoticeHours = minNoticeHours; }
    public int getMaxHorizonDays() { return maxHorizonDays; }
    public void setMaxHorizonDays(int maxHorizonDays) { this.maxHorizonDays = maxHorizonDays; }
    public CancellationMode getCancellationMode() { return cancellationMode; }
    public void setCancellationMode(CancellationMode mode) { this.cancellationMode = mode; }
    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
}
