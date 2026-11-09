package com.example.demo;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timetable-entries")
public class TimetableEntryController {

    private final TimetableEntryRepository entries;
    private final ClassGroupRepository classes;
    private final SubjectRepository subjects;
    private final RoomRepository rooms;
    private final TimeSlotRepository slots;

    public TimetableEntryController(TimetableEntryRepository entries,
                                    ClassGroupRepository classes,
                                    SubjectRepository subjects,
                                    RoomRepository rooms,
                                    TimeSlotRepository slots) {
        this.entries = entries;
        this.classes = classes;
        this.subjects = subjects;
        this.rooms = rooms;
        this.slots = slots;
    }

    @GetMapping
    public List<TimetableEntry> all() {
        return entries.findAllByOrderByTimeSlotDayAscTimeSlotPeriodAsc();
    }

    @GetMapping("/{id}")
    public TimetableEntry one(@PathVariable Long id) {
        return entries.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimetableEntry create(@Valid @RequestBody TimetableEntryRequest request) {
        return entries.save(toEntry(request));
    }

    @PutMapping("/{id}")
    public TimetableEntry update(@PathVariable Long id, @Valid @RequestBody TimetableEntryRequest request) {
        TimetableEntry current = one(id);
        TimetableEntry replacement = toEntry(request);
        current.setClassGroup(replacement.getClassGroup());
        current.setSubject(replacement.getSubject());
        current.setTeacher(replacement.getTeacher());
        current.setRoom(replacement.getRoom());
        current.setTimeSlot(replacement.getTimeSlot());
        return entries.save(current);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        entries.delete(one(id));
    }

    private TimetableEntry toEntry(TimetableEntryRequest request) {
        ClassGroup classGroup = classes.findById(request.classGroupId())
                .orElseThrow(() -> new StudentNotFoundException(request.classGroupId()));
        Subject subject = subjects.findById(request.subjectId())
                .orElseThrow(() -> new StudentNotFoundException(request.subjectId()));
        Room room = rooms.findById(request.roomId())
                .orElseThrow(() -> new StudentNotFoundException(request.roomId()));
        TimeSlot slot = slots.findById(request.timeSlotId())
                .orElseThrow(() -> new StudentNotFoundException(request.timeSlotId()));
        return new TimetableEntry(classGroup, subject, subject.getTeacher(), room, slot);
    }
}
