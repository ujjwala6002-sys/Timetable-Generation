package com.example.demo;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @PostMapping("/generate")
    public Map<String, Integer> generate() {
        return timetableService.generate();
    }

    @GetMapping("/class/{id}")
    public List<TimetableEntry> byClass(@PathVariable Long id) {
        return timetableService.findByClass(id);
    }

    @GetMapping("/room/{id}")
    public List<TimetableEntry> byRoom(@PathVariable Long id) {
        return timetableService.findByRoom(id);
    }
}
