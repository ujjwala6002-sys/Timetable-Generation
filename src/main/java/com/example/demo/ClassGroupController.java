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
@RequestMapping("/api/classes")
public class ClassGroupController {
    private final ClassGroupRepository repository;
    public ClassGroupController(ClassGroupRepository repository) { this.repository = repository; }
    @GetMapping public List<ClassGroup> all() { return repository.findAll(); }
    @GetMapping("/{id}") public ClassGroup one(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ClassGroup create(@Valid @RequestBody ClassGroup item) { return repository.save(item); }
    @PutMapping("/{id}") public ClassGroup update(@PathVariable Long id, @Valid @RequestBody ClassGroup item) { ClassGroup current = one(id); current.setName(item.getName()); return repository.save(current); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { repository.delete(one(id)); }
}
