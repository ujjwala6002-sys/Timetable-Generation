package com.example.demo;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataSeeder {

    @Bean
    CommandLineRunner seedTimetableData(TeacherRepository teachers,
                                       SubjectRepository subjects,
                                       RoomRepository rooms,
                                       ClassGroupRepository classes,
                                       TimeSlotRepository slots) {
        return args -> {
            if (teachers.count() != 0 || subjects.count() != 0 || rooms.count() != 0
                    || classes.count() != 0) {
                return;
            }

            Teacher taylor = teachers.save(new Teacher("Jordan Taylor"));
            Teacher singh = teachers.save(new Teacher("Priya Singh"));
            Teacher chen = teachers.save(new Teacher("Alex Chen"));

            subjects.saveAll(Arrays.asList(
                    new Subject("Mathematics", 5, taylor),
                    new Subject("Science", 4, singh),
                    new Subject("English", 4, chen),
                    new Subject("History", 3, taylor)));

            rooms.saveAll(Arrays.asList(
                    new Room("Room 101", 30),
                    new Room("Room 102", 28),
                    new Room("Science Lab", 24)));

            classes.saveAll(Arrays.asList(new ClassGroup("Class 7A"), new ClassGroup("Class 7B")));

            if (slots.count() == 0) {
                for (TimeSlot.Day day : TimeSlot.Day.values()) {
                    for (int period = 1; period <= 6; period++) {
                        slots.save(new TimeSlot(day, period));
                    }
                }
            }
        };
    }
}
