package com.example.demo;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student create(Student student) {
        normalize(student);
        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new DuplicateStudentEmailException(
                    "A student with email " + student.getEmail() + " already exists");
        }
        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }

    public Student update(Long id, Student updates) {
        Student existing = findById(id);
        normalize(updates);
        if (studentRepository.existsByEmailAndIdNot(updates.getEmail(), id)) {
            throw new DuplicateStudentEmailException(
                    "A student with email " + updates.getEmail() + " already exists");
        }
        existing.setName(updates.getName());
        existing.setEmail(updates.getEmail());
        existing.setCourse(updates.getCourse());
        existing.setPhoneNumber(updates.getPhoneNumber());
        return studentRepository.save(existing);
    }

    public void delete(Long id) {
        studentRepository.delete(findById(id));
    }

    private void normalize(Student student) {
        student.setName(student.getName().trim());
        student.setEmail(student.getEmail().trim().toLowerCase(Locale.ROOT));
        student.setCourse(student.getCourse().trim());
        student.setPhoneNumber(student.getPhoneNumber().trim());
    }
}