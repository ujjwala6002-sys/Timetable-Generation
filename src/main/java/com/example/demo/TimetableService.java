package com.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimetableService {

    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final RoomRepository roomRepository;
    private final ClassGroupRepository classGroupRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TimetableEntryRepository entryRepository;

    public TimetableService(TeacherRepository teacherRepository,
                            SubjectRepository subjectRepository,
                            RoomRepository roomRepository,
                            ClassGroupRepository classGroupRepository,
                            TimeSlotRepository timeSlotRepository,
                            TimetableEntryRepository entryRepository) {
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.roomRepository = roomRepository;
        this.classGroupRepository = classGroupRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public Map<String, Integer> generate() {
        List<ClassGroup> classes = classGroupRepository.findAll();
        List<Subject> subjects = subjectRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<TimeSlot> slots = timeSlotRepository.findAllByOrderByDayAscPeriodAsc();

        entryRepository.deleteAllInBatch();
        if (classes.isEmpty() || subjects.isEmpty() || rooms.isEmpty() || slots.isEmpty()) {
            return summary(0, 0);
        }

        Set<ResourceSlot> occupiedClasses = new HashSet<>();
        Set<ResourceSlot> occupiedTeachers = new HashSet<>();
        Set<ResourceSlot> occupiedRooms = new HashSet<>();
        Map<ClassDay, Integer> classDayLoads = new HashMap<>();
        Map<ClassPeriod, Integer> classPeriodLoads = new HashMap<>();
        Map<ClassSubjectDay, Integer> subjectDayLoads = new HashMap<>();
        Map<ClassSubjectPeriod, Integer> subjectPeriodLoads = new HashMap<>();
        Map<ClassDay, Map<Integer, Long>> classDaySubjects = new HashMap<>();
        Map<Long, Integer> roomBookings = new HashMap<>();
        List<TimetableEntry> scheduled = new ArrayList<>();
        int requested = 0;
        int unscheduled = 0;

        for (ClassGroup classGroup : classes) {
            for (Subject subject : subjects) {
                int lessonCount = Math.max(0, subject.getPeriodsPerWeek());
                requested += lessonCount;
                for (int lesson = 0; lesson < lessonCount; lesson++) {
                        SlotRoom assignment = findSlot(classGroup, subject, rooms, slots,
                            occupiedClasses, occupiedTeachers, occupiedRooms,
                            classDayLoads, classPeriodLoads, subjectDayLoads, subjectPeriodLoads,
                            classDaySubjects, roomBookings);
                        if (assignment == null) {
                        List<TimeSlot> shuffledSlots = new ArrayList<>(slots);
                        Collections.shuffle(shuffledSlots);
                        assignment = findSlot(classGroup, subject, rooms, shuffledSlots,
                            occupiedClasses, occupiedTeachers, occupiedRooms,
                                classDayLoads, classPeriodLoads, subjectDayLoads, subjectPeriodLoads,
                                classDaySubjects, roomBookings);
                    }
                        if (assignment == null) {
                        unscheduled++;
                        continue;
                    }

                        TimeSlot selected = assignment.slot();
                        Room selectedRoom = assignment.room();
                        scheduled.add(new TimetableEntry(classGroup, subject, subject.getTeacher(), selectedRoom, selected));
                        occupiedClasses.add(new ResourceSlot(classGroup.getId(), selected.getId()));
                        occupiedTeachers.add(new ResourceSlot(subject.getTeacher().getId(), selected.getId()));
                        occupiedRooms.add(new ResourceSlot(selectedRoom.getId(), selected.getId()));

                        ClassDay classDay = new ClassDay(classGroup.getId(), selected.getDay());
                        classDayLoads.merge(classDay, 1, Integer::sum);
                        classPeriodLoads.merge(new ClassPeriod(classGroup.getId(), selected.getPeriod()), 1, Integer::sum);
                        subjectDayLoads.merge(new ClassSubjectDay(classGroup.getId(), subject.getId(), selected.getDay()), 1, Integer::sum);
                        subjectPeriodLoads.merge(new ClassSubjectPeriod(classGroup.getId(), subject.getId(), selected.getPeriod()), 1, Integer::sum);
                        classDaySubjects.computeIfAbsent(classDay, ignored -> new HashMap<>())
                            .put(selected.getPeriod(), subject.getId());
                        roomBookings.merge(selectedRoom.getId(), 1, Integer::sum);
                }
            }
        }

        entryRepository.saveAll(scheduled);
        Map<String, Integer> result = summary(scheduled.size(), unscheduled);
        result.put("requestedCount", requested);
        return result;
    }

    private SlotRoom findSlot(ClassGroup classGroup, Subject subject, List<Room> rooms,
                              List<TimeSlot> slots, Set<ResourceSlot> occupiedClasses,
                              Set<ResourceSlot> occupiedTeachers, Set<ResourceSlot> occupiedRooms,
                              Map<ClassDay, Integer> classDayLoads,
                                  Map<ClassPeriod, Integer> classPeriodLoads,
                              Map<ClassSubjectDay, Integer> subjectDayLoads,
                                  Map<ClassSubjectPeriod, Integer> subjectPeriodLoads,
                              Map<ClassDay, Map<Integer, Long>> classDaySubjects,
                              Map<Long, Integer> roomBookings) {
        List<TimeSlot> candidates = new ArrayList<>(slots);
        candidates.sort(Comparator
                        .comparingInt((TimeSlot slot) -> classPeriodLoads.getOrDefault(
                            new ClassPeriod(classGroup.getId(), slot.getPeriod()), 0))
                        .thenComparingInt(slot -> classDayLoads.getOrDefault(
                            new ClassDay(classGroup.getId(), slot.getDay()), 0))
                        .thenComparingInt(slot -> subjectPeriodLoads.getOrDefault(
                            new ClassSubjectPeriod(classGroup.getId(), subject.getId(), slot.getPeriod()), 0))
                .thenComparingInt(slot -> subjectDayLoads.getOrDefault(
                        new ClassSubjectDay(classGroup.getId(), subject.getId(), slot.getDay()), 0))
                .thenComparingInt(slot -> slot.getDay().ordinal())
                .thenComparingInt(TimeSlot::getPeriod));

        for (TimeSlot slot : candidates) {
            ResourceSlot resourceSlot = new ResourceSlot(classGroup.getId(), slot.getId());
            if (occupiedClasses.contains(resourceSlot)
                    || occupiedTeachers.contains(new ResourceSlot(subject.getTeacher().getId(), slot.getId()))) {
                continue;
            }

            ClassDay classDay = new ClassDay(classGroup.getId(), slot.getDay());
            ClassSubjectDay classSubjectDay = new ClassSubjectDay(classGroup.getId(), subject.getId(), slot.getDay());
            if (subjectDayLoads.getOrDefault(classSubjectDay, 0) >= 2
                    || wouldCreateThreeConsecutive(classDaySubjects.getOrDefault(classDay, Map.of()),
                    slot.getPeriod(), subject.getId())) {
                continue;
            }

            Room leastBookedFreeRoom = rooms.stream()
                    .filter(room -> !occupiedRooms.contains(new ResourceSlot(room.getId(), slot.getId())))
                    .min(Comparator.comparingInt((Room room) -> roomBookings.getOrDefault(room.getId(), 0))
                            .thenComparing(Room::getId))
                    .orElse(null);
            if (leastBookedFreeRoom != null) {
                return new SlotRoom(slot, leastBookedFreeRoom);
            }
        }
        return null;
    }

    private boolean wouldCreateThreeConsecutive(Map<Integer, Long> periods, int period, Long subjectId) {
        for (int start = Math.max(1, period - 2); start <= Math.min(period, 4); start++) {
            boolean allSameSubject = true;
            for (int current = start; current < start + 3; current++) {
                Long currentSubject = current == period ? subjectId : periods.get(current);
                if (!subjectId.equals(currentSubject)) {
                    allSameSubject = false;
                    break;
                }
            }
            if (allSameSubject) {
                return true;
            }
        }
        return false;
    }

    private record ResourceSlot(Long resourceId, Long slotId) { }
    private record ClassDay(Long classId, TimeSlot.Day day) { }
    private record ClassPeriod(Long classId, int period) { }
    private record ClassSubjectDay(Long classId, Long subjectId, TimeSlot.Day day) { }
    private record ClassSubjectPeriod(Long classId, Long subjectId, int period) { }
    private record SlotRoom(TimeSlot slot, Room room) { }

    private Map<String, Integer> summary(int scheduled, int unscheduled) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("scheduledCount", scheduled);
        result.put("unscheduledCount", unscheduled);
        return result;
    }

    @Transactional(readOnly = true)
    public List<TimetableEntry> findByClass(Long classId) {
        if (!classGroupRepository.existsById(classId)) {
            throw new StudentNotFoundException(classId);
        }
        return entryRepository.findAllByClassGroupIdOrderByTimeSlotDayAscTimeSlotPeriodAsc(classId);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntry> findByRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new StudentNotFoundException(roomId);
        }
        return entryRepository.findAllByRoomIdOrderByTimeSlotDayAscTimeSlotPeriodAsc(roomId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardStats() {
        List<TimetableEntry> entries = entryRepository.findAll();
        Set<String> classes = new HashSet<>();
        Set<String> teachers = new HashSet<>();
        Set<String> rooms = new HashSet<>();
        int conflicts = 0;
        for (TimetableEntry entry : entries) {
            String slotId = String.valueOf(entry.getTimeSlot().getId());
            if (!classes.add(entry.getClassGroup().getId() + ":" + slotId)) conflicts++;
            if (!teachers.add(entry.getTeacher().getId() + ":" + slotId)) conflicts++;
            if (!rooms.add(entry.getRoom().getId() + ":" + slotId)) conflicts++;
        }

        int totalRoomSlots = (int) roomRepository.count() * (int) timeSlotRepository.count();
        double utilization = totalRoomSlots == 0 ? 0.0 : entries.size() * 100.0 / totalRoomSlots;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("teachers", teacherRepository.count());
        result.put("classes", classGroupRepository.count());
        result.put("rooms", roomRepository.count());
        result.put("subjects", subjectRepository.count());
        result.put("entries", entries.size());
        result.put("conflicts", conflicts);
        result.put("roomUtilizationPercent", Math.round(utilization * 100.0) / 100.0);
        return result;
    }
}
