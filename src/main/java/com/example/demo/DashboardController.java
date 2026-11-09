package com.example.demo;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TimetableService timetableService;

    public DashboardController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return timetableService.dashboardStats();
    }
}
