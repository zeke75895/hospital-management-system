package com.example.hospital_management_system.repository;

import com.example.hospital_management_system.entity.Schedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Schedules that apply to doctorId on the given calendar date: any one-off Schedule whose
     * specificDate matches exactly, plus any recurring Schedule whose dayOfWeek matches. Both can
     * come back at once (e.g. a recurring Monday slot plus a specificDate override for that same
     * Monday) — resolving which one wins is a service-layer decision, not a repository concern.
     */
    default List<Schedule> findSchedulesForDoctorOnDate(Long doctorId, LocalDate date) {
        return findSchedulesForDoctorAndDayOfWeek(doctorId, date, date.getDayOfWeek());
    }

    @Query(
            "SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId "
                    + "AND (s.specificDate = :date OR s.dayOfWeek = :dayOfWeek)")
    List<Schedule> findSchedulesForDoctorAndDayOfWeek(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") DayOfWeek dayOfWeek);
}
