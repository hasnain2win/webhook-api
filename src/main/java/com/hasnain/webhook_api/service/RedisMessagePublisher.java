package com.hasnain.webhook_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RedisMessagePublisher {

    @Autowired
    private final RedisTemplate<String, String> redisTemplate;

    // Ensure RedisTemplate is injected via the constructor
    public RedisMessagePublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, String message) {
     Long id=   redisTemplate.convertAndSend(channel, message);
        System.out.println(id);
    }

    public void publishImportantData(String jsonPayload) throws JsonProcessingException {
        // Extract important data from the payload
        ObjectMapper mapper = new ObjectMapper();
        HashMap eventPayload = mapper.readValue(jsonPayload, HashMap.class);

        String repositoryName = (String) ((Map<String, Object>) eventPayload.get("repository")).get("full_name");
        String branchName = ((String) eventPayload.get("ref")).replace("refs/heads/", "");
        String commitId = (String) eventPayload.get("after");
        String commitMessage = (String) ((Map<String, Object>) eventPayload.get("head_commit")).get("message");
        String authorName = (String) ((Map<String, Object>) ((Map<String, Object>) eventPayload.get("head_commit")).get("author")).get("name");

        // Create a map of the important data
        Map<String, Object> importantData = new HashMap<>();
        importantData.put("repository", repositoryName);
        importantData.put("branch", branchName);
        importantData.put("commit_id", commitId);
        importantData.put("commit_message", commitMessage);
        importantData.put("author", authorName);

        // Generate the channel name
        String channelName = String.format("creator:%s:commit:%s", authorName, commitId);

        // Publish the important data
        redisTemplate.convertAndSend(channelName, importantData);

        System.out.println("Published to channel: " + channelName + " with data: " + importantData);
    }
}
