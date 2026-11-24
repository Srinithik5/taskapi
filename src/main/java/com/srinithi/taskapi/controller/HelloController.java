package com.srinithi.taskapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// TEMPORARY: proves the server boots end-to-end. Deleted once real endpoints exist (Phase 6).
@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello from taskapi");
    }
}
