package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:timetabletests;DB_CLOSE_DELAY=-1")
class TimetableServiceTests {

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private TimetableEntryRepository entryRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void generatedTimetableHasNoRoomTeacherOrClassConflicts() {
        entryRepository.deleteAll();
        timeSlotRepository.deleteAll();
        subjectRepository.deleteAll();
        classGroupRepository.deleteAll();
        roomRepository.deleteAll();
        teacherRepository.deleteAll();

        Teacher firstTeacher = teacherRepository.save(new Teacher("Taylor"));
        Teacher secondTeacher = teacherRepository.save(new Teacher("Singh"));
        Subject math = subjectRepository.save(new Subject("Math", 5, firstTeacher));
        Subject science = subjectRepository.save(new Subject("Science", 4, secondTeacher));
        roomRepository.save(new Room("Room A", 30));
        roomRepository.save(new Room("Room B", 30));
        ClassGroup firstClass = classGroupRepository.save(new ClassGroup("Class A"));
        classGroupRepository.save(new ClassGroup("Class B"));
        for (TimeSlot.Day day : TimeSlot.Day.values()) {
            for (int period = 1; period <= 6; period++) {
                timeSlotRepository.save(new TimeSlot(day, period));
            }
        }

        var summary = timetableService.generate();
        var entries = entryRepository.findAll();
        Set<String> classSlots = new HashSet<>();
        Set<String> teacherSlots = new HashSet<>();
        Set<String> roomSlots = new HashSet<>();
        Map<String, Integer> subjectDayCounts = new HashMap<>();
        Map<String, Map<Integer, Long>> classDayPeriods = new HashMap<>();
        Map<Integer, Integer> firstClassPeriodCounts = new HashMap<>();
        Map<Long, Set<Integer>> subjectPeriods = new HashMap<>();

        for (TimetableEntry entry : entries) {
            String slot = entry.getTimeSlot().getId().toString();
            assertTrue(classSlots.add(entry.getClassGroup().getId() + ":" + slot), "Class double-booked");
            assertTrue(teacherSlots.add(entry.getTeacher().getId() + ":" + slot), "Teacher double-booked");
            assertTrue(roomSlots.add(entry.getRoom().getId() + ":" + slot), "Room double-booked");

            String classDay = entry.getClassGroup().getId() + ":" + entry.getTimeSlot().getDay();
            String subjectDay = classDay + ":" + entry.getSubject().getId();
            subjectDayCounts.merge(subjectDay, 1, Integer::sum);
            classDayPeriods.computeIfAbsent(classDay, ignored -> new HashMap<>())
                    .put(entry.getTimeSlot().getPeriod(), entry.getSubject().getId());
                if (entry.getClassGroup().getId().equals(firstClass.getId())) {
                firstClassPeriodCounts.merge(entry.getTimeSlot().getPeriod(), 1, Integer::sum);
                subjectPeriods.computeIfAbsent(entry.getSubject().getId(), ignored -> new HashSet<>())
                    .add(entry.getTimeSlot().getPeriod());
                }
        }

        assertEquals(18, summary.get("scheduledCount"));
        assertEquals(0, summary.get("unscheduledCount"));
        assertEquals(9, entries.stream().filter(entry -> entry.getClassGroup().getId().equals(firstClass.getId())).count());
        assertTrue(entries.stream().anyMatch(entry -> entry.getSubject().getId().equals(math.getId())));
        assertTrue(entries.stream().anyMatch(entry -> entry.getSubject().getId().equals(science.getId())));
        assertEquals(6, firstClassPeriodCounts.size(), "Lessons should be spread across all six periods");
        assertTrue(subjectPeriods.values().stream().allMatch(periods -> periods.size() > 1),
            "A subject should not use only one period throughout the week");
        assertTrue(subjectDayCounts.values().stream().allMatch(count -> count <= 2),
                "A class must not have more than two lessons of a subject on one day");
        for (Map<Integer, Long> periods : classDayPeriods.values()) {
            for (int period = 1; period <= 4; period++) {
                Long subjectId = periods.get(period);
                if (subjectId != null) {
                    assertTrue(!(subjectId.equals(periods.get(period + 1))
                                    && subjectId.equals(periods.get(period + 2))),
                            "A subject must not occupy more than two consecutive periods");
                }
            }
        }
    }
}
