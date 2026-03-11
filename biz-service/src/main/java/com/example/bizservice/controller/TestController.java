package com.example.bizservice.controller;

import com.example.bizservice.model.TestResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a sample endpoint for quick connectivity verification.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * Returns a sample response to validate request routing.
     *
     * @param name optional caller name.
     * @return test endpoint response.
     */
    @GetMapping("/test")
    public TestResponse test(@RequestParam(defaultValue = "world") String name) {
        return new TestResponse(
                "test",
                "Hello, %s! biz-service is responding normally.".formatted(name),
                name,
                Instant.now());
    }
}
