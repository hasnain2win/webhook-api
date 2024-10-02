package com.membermismatch.contact.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.membermismatch.contact.service.ContactSummaryPublishService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
                LOGGER.info("No data node found in the payload");
                return;
            }

            String ucid = getJsonValue(dataNode, "UCID", "ucid");
            String summaryText = getJsonValue(dataNode, "SummaryText", "summaryText");
            boolean isSuccess = dataNode.path("IsSuccess").asBoolean(true);
            String failedReason = dataNode.path("FailedReason").asText("");

            if (ucid == null) {
                LOGGER.info("No UCID found in the payload");
                return;
            }

            String channelName = String.format("channel-%s", ucid);
            if (!isSuccess || !failedReason.isEmpty()) {
                summaryText = String.format("FailedSummary: %s", failedReason);
            }

            if (summaryText != null) {
                redisTemplate.convertAndSend(channelName, summaryText);
                LOGGER.info("Published to channel: {} with data: {}", channelName, summaryText);
            }

        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing JSON payload: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        LOGGER.info("End of publishContactSummary method");
    }

    private String getJsonValue(JsonNode dataNode, String primaryKey, String secondaryKey) {
        JsonNode valueNode = dataNode.path(primaryKey);
        if (valueNode.isMissingNode()) {
            valueNode = dataNode.path(secondaryKey);
        }
        return valueNode.isMissingNode() ? null : valueNode.asText();
    }
}
