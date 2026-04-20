package com.aigrs.backend.controller;

import com.aigrs.backend.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, String>>> root() {
        Map<String, String> data = Map.of(
                "service", "AIGRS Backend",
                "status", "UP",
                "health", "/actuator/health"
        );
        return ResponseEntity.ok(ApiResponse.success("AIGRS backend is running", data));
    }
}
