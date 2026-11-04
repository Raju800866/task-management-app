package com.taskapp.taskservice.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // Public; used by Docker and the AWS load balancer health checks
    @GetMapping("/api/tasks/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
