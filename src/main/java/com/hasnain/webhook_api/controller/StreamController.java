package com.hasnain.webhook_api.controller;

import com.hasnain.webhook_api.StreamRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class StreamController {

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);
    private final Map<String, MessageListener> listeners = new ConcurrentHashMap<>();

    @GetMapping(value = "/webapp/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@RequestBody StreamRequest streamRequest) {
        String channel = String.format("summary-%s:%s", streamRequest.getCallerId(), streamRequest.getAgentId());
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30-minute timeout

        logger.info("Opening SSE connection for channel: {}", channel);

        MessageListener messageListener = listeners.computeIfAbsent(channel, key -> new MessageListenerAdapter(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    String messageContent = new String(message.getBody());
                    logger.info("Received message: {}", messageContent);
                    emitter.send(SseEmitter.event().data(messageContent));
                } catch (IOException e) {
                    logger.error("Error while sending message to client", e);
                    emitter.completeWithError(e);
                }
            }
        }));

        redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(channel));

        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for channel: {}", channel);
            redisMessageListenerContainer.removeMessageListener(messageListener);
            listeners.remove(channel);
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for channel: {}", channel);
            emitter.complete();
            redisMessageListenerContainer.removeMessageListener(messageListener);
            listeners.remove(channel);
        });

        return emitter;
    }
}
