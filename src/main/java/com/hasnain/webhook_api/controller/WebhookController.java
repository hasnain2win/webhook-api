package com.hasnain.webhook_api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hasnain.webhook_api.service.RedisMessagePublisher;
import com.hasnain.webhook_api.utils.ChannelNameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WebhookController {

    private final RedisMessagePublisher redisMessagePublisher;

    @Autowired
    public WebhookController(RedisMessagePublisher redisMessagePublisher) {
        this.redisMessagePublisher = redisMessagePublisher;
    }

    @PostMapping("/webapp/api/webhook")
    public ResponseEntity<String> handleWebhookEvent( @RequestBody String eventPayload) throws JsonProcessingException {
        // Extract caller and agent IDs from payload


        // Create a unique channel name for this caller-agent pair
       // String channel = "summary:" + callerId + ":" + agentId;

        // Publish the payload to the unique channel
        redisMessagePublisher.publishImportantData( eventPayload);

        return ResponseEntity.ok("Event processed successfully");
    }

    private String extractCallerId(String payload) {
        // Implement logic to extract caller ID from the payload
        return "callerId";
    }

    private String extractAgentId(String payload) {
        // Implement logic to extract agent ID from the payload
        return "agentId";
    }
}

