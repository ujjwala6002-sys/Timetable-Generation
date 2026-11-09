package com.example.demo;

public class DuplicateStudentEmailException extends RuntimeException {
    public DuplicateStudentEmailException(String message) {
        super(message);
    }
}
