package com.example.demo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:studenttests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class StudentApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void clearStudents() {
        studentRepository.deleteAll();
    }

        @Test
        void servesDashboardAtRoot() throws Exception {
                mockMvc.perform(get("/index.html"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                                .andExpect(content().string(containsString("Student list")));
        }

    @Test
    void createsListsFindsUpdatesAndDeletesStudent() throws Exception {
        String student = """
                {"name":"Maya Chen","email":"maya@example.com","course":"Computer Science","phoneNumber":"+1 555 010 2040"}
                """;

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(student))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Maya Chen"));

        Long id = studentRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("maya@example.com"));
        mockMvc.perform(get("/api/students/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+1 555 010 2040"));

        mockMvc.perform(put("/api/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maya C.","email":"maya@example.com","course":"Design","phoneNumber":"5550102040"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maya C."))
                .andExpect(jsonPath("$.course").value("Design"));

        mockMvc.perform(delete("/api/students/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/students/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("was not found")));
    }

    @Test
    void rejectsInvalidStudentDetails() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","course":"","phoneNumber":"abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(content().string(containsString("Please correct")));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String student = """
                {"name":"Maya Chen","email":"maya@example.com","course":"Computer Science","phoneNumber":"5550102040"}
                """;
        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(student))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(student))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }
}
