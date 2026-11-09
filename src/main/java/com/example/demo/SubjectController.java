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
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectRepository repository;
    private final TeacherRepository teacherRepository;
    public SubjectController(SubjectRepository repository, TeacherRepository teacherRepository) { this.repository = repository; this.teacherRepository = teacherRepository; }
    @GetMapping public List<Subject> all() { return repository.findAll(); }
    @GetMapping("/{id}") public Subject one(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Subject create(@Valid @RequestBody Subject item) { item.setTeacher(teacher(item.getTeacher())); return repository.save(item); }
    @PutMapping("/{id}") public Subject update(@PathVariable Long id, @Valid @RequestBody Subject item) { Subject current = one(id); current.setName(item.getName()); current.setPeriodsPerWeek(item.getPeriodsPerWeek()); current.setTeacher(teacher(item.getTeacher())); return repository.save(current); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { repository.delete(one(id)); }
    private Teacher teacher(Teacher value) { return teacherRepository.findById(value.getId()).orElseThrow(() -> new StudentNotFoundException(value.getId())); }
}
