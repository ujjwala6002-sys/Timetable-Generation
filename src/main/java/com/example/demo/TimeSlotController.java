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
@RequestMapping("/api/time-slots")
public class TimeSlotController {
    private final TimeSlotRepository repository;
    public TimeSlotController(TimeSlotRepository repository) { this.repository = repository; }
    @GetMapping public List<TimeSlot> all() { return repository.findAllByOrderByDayAscPeriodAsc(); }
    @GetMapping("/{id}") public TimeSlot one(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TimeSlot create(@Valid @RequestBody TimeSlot item) { return repository.save(item); }
    @PutMapping("/{id}") public TimeSlot update(@PathVariable Long id, @Valid @RequestBody TimeSlot item) { TimeSlot current = one(id); current.setDay(item.getDay()); current.setPeriod(item.getPeriod()); return repository.save(current); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { repository.delete(one(id)); }
}
