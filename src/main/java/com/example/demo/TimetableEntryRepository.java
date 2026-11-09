package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findAllByClassGroupIdOrderByTimeSlotDayAscTimeSlotPeriodAsc(Long classGroupId);

    List<TimetableEntry> findAllByRoomIdOrderByTimeSlotDayAscTimeSlotPeriodAsc(Long roomId);

    List<TimetableEntry> findAllByOrderByTimeSlotDayAscTimeSlotPeriodAsc();
}
