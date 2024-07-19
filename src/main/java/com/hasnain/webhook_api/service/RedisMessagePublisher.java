package com.hasnain.webhook_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RedisMessagePublisher {

    @Autowired
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // Ensure RedisTemplate is injected via the constructor
    public RedisMessagePublisher(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, String message) {
     Long id= redisTemplate.convertAndSend(channel, message).block();
        System.out.println(id);
    }

    public void publishImportantData(String jsonPayload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> eventPayload = mapper.readValue(jsonPayload, Map.class);

            String repositoryName = (String) ((Map<String, Object>) eventPayload.get("repository")).get("full_name");
            String branchName = ((String) eventPayload.get("ref")).replace("refs/heads/", "");
            String commitId = (String) eventPayload.get("after");
            String commitMessage = (String) ((Map<String, Object>) eventPayload.get("head_commit")).get("message");
            String authorName = (String) ((Map<String, Object>) ((Map<String, Object>) eventPayload.get("head_commit")).get("author")).get("name");

            Map<String, Object> importantData = new HashMap<>();
            importantData.put("repository", repositoryName);
            importantData.put("branch", branchName);
            importantData.put("commit_id", commitId);
            importantData.put("commit_message", commitMessage);
            importantData.put("author", authorName);

            String channelName = String.format("summary-creator:%s", authorName);
            String message = mapper.writeValueAsString(importantData);
            redisTemplate.convertAndSend(channelName, message)
                    .doOnSuccess(id -> System.out.println("Published to channel: " + channelName + " with data: " + importantData))
                    .doOnError(error -> System.err.println("Error publishing important data: " + error.getMessage()))
                    .subscribe();

        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON payload: " + e.getMessage());
        }
    }

}
