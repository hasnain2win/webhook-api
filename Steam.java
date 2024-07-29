package com.membermismatch.contact.controller;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class ContactSummaryStreamController {

    private static final Logger logger = LoggerFactory.getLogger(ContactSummaryStreamController.class);

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> emitterStatus = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping(value = "/contactSummaryStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage() {
        logger.info("Start of streamMessage method: ContactSummaryStreamController");

        String channelName = "ac-summary-event";
        SseEmitter emitter = new SseEmitter(0L); // Unlimited timeout
        emitters.put(channelName, emitter);
        emitterStatus.put(channelName, false); // Initially not completed

        ChannelTopic topic = new ChannelTopic(channelName);

        MessageListener listener = (Message message, byte[] pattern) -> {
            String data = new String(message.getBody());
            logger.info("Received message on channel {}: {}", StringEscapeUtils.escapeJava(channelName), StringEscapeUtils.escapeJava(data));

            executor.execute(() -> sendMessage(emitter, data, channelName));
        };

        redisMessageListenerContainer.addMessageListener(listener, topic);

        emitter.onCompletion(() -> cleanupEmitter(channelName, topic, listener));
        emitter.onTimeout(() -> cleanupEmitter(channelName, topic, listener));
        logger.info("End of streamMessage method :ContactSummaryStreamController");
        return emitter;
    }

    private void sendMessage(SseEmitter emitter, String data, String channel) {
        try {
            // Used a synchronized block to ensure thread-safe access to emitter status
            synchronized (emitterStatus) {
                if (!emitterStatus.getOrDefault(channel, true)) { // Check if emitter is completed
                    emitter.send(SseEmitter.event().data(data));
                }
            }
        } catch (IOException e) {
            logger.error("Error sending SSE message for channel {}: {}", StringEscapeUtils.escapeJava(channel), e.getMessage());
            cleanupEmitter(channel, new ChannelTopic(channel), null); // Clean up on error
        }
    }

    private void cleanupEmitter(String channel, ChannelTopic topic, MessageListener listener) {
        logger.info("Cleaning up SSE emitter for channel: {}", StringEscapeUtils.escapeJava(channel));
        emitters.remove(channel);
        emitterStatus.remove(channel); // Remove status entry
        if (listener != null) {
            redisMessageListenerContainer.removeMessageListener(listener, topic);
        }
    }
    private String determineChannelName(String callId, String agentId, String profileType) {
        if (profileType != null && profileType.contains("pbm")) {
            return String.format("channel-pbm-%s-%s", callId, agentId);
        } else if (profileType != null && profileType.contains("pharmacy")) {
            return String.format("channel-pharmacy-%s-%s", callId, agentId);
        } else {
            return String.format("channel-%s-%s", callId, agentId);
        }
    }
}
