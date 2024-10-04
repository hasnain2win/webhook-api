package com.membermismatch.contact.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.membermismatch.contact.service.ContactSummaryPublishService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ContactSummaryPublishServiceImpl implements ContactSummaryPublishService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger LOGGER = LogManager.getLogger(ContactSummaryPublishServiceImpl.class);

    public ContactSummaryPublishServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void publishContactSummary(String jsonPayload) {
        LOGGER.info("Started publishContactSummary method");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode dataNode = objectMapper.readTree(jsonPayload).path("data");

            if (dataNode.isMissingNode()) {
                LOGGER.warn("No 'data' node found in the payload");
                return;
            }

            String ucid = getJsonValue(dataNode, "UCID", "ucid");
            if (ucid == null) {
                LOGGER.warn("No UCID found in the payload");
                return;
            }

            String summaryText = getJsonValue(dataNode, "SummaryText", "summaryText");
            if (summaryText == null || summaryText.trim().isEmpty()) {
                LOGGER.warn("SummaryText is null or empty, nothing to publish");
                return;
            }

            boolean isSuccess = dataNode.path("IsSuccess").asBoolean(true);
            String failedReason = dataNode.path("FailedReason").asText("");

            // If there is a failure, override the summaryText with failure reason
            if (!isSuccess || !failedReason.isEmpty()) {
                summaryText = String.format("FailedSummary: %s", failedReason);
            }

            // Stream name format: "channel-<ucid>"
            String streamName = String.format("channel-%s", ucid);

            // Convert data to a map and publish to Redis stream
            Map<String, String> messageData = new HashMap<>();
            messageData.put("summaryText", summaryText);

            // Publish to Redis stream
            redisTemplate.opsForStream().add(MapRecord.create(streamName, messageData));

            LOGGER.info("Published to redis channel: {} with data: {}", streamName, summaryText);

        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing JSON payload: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing JSON payload", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during publishing", e);
        } finally {
            LOGGER.info("End of publishContactSummary method");
        }
    }

    private String getJsonValue(JsonNode dataNode, String primaryKey, String secondaryKey) {
        JsonNode valueNode = dataNode.path(primaryKey);
        if (valueNode.isMissingNode()) {
            valueNode = dataNode.path(secondaryKey);
        }
        return valueNode.isMissingNode() ? null : valueNode.asText();
    }
}
