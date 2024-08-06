package com.membermismatch.contact.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Component
public class ContactSummaryWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(50);
    private static final Logger logger = LoggerFactory.getLogger(ContactSummaryWebSocketHandler.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("Starting afterConnectionEstablished method: ContactSummaryWebSocketHandler");
        String channelName = getChannelNameFromSession(session);
        if (channelName != null) {
            sessions.put(channelName, session);

            // Set up Redis message listener for the channel
            ChannelTopic topic = new ChannelTopic(channelName);
            synchronized (this) {
                MessageListener listener = (message, pattern) -> {
                    String data = new String(message.getBody());
                    logger.debug("Message received from Redis: {}", data); // Add debug log
                    executor.execute(() -> {
                        try {
                            logger.info("Received message on channel {}: {}", channelName, data);
                            session.sendMessage(new TextMessage(data));
                        } catch (IOException e) {
                            logger.error("Failed to send message to WebSocket session {}", e.getMessage());
                        }
                    });
                };

                redisMessageListenerContainer.addMessageListener(listener, topic);
            }
        } else {
            session.close(); // Close connection if no channel name is provided
            logger.error("connection closed");
        }
        logger.info("Ending afterConnectionEstablished method: ContactSummaryWebSocketHandler");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String channelName = getChannelNameFromSession(session);
        if (channelName != null) {
            sessions.remove(channelName);
            // Clean up Redis listeners
            ChannelTopic topic = new ChannelTopic(channelName);
            redisMessageListenerContainer.removeMessageListener((message, pattern) -> {}, topic);
        }
    }

    private String getChannelNameFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null) {
                Map<String, String> queryParams = splitQuery(query);
                return queryParams.get("channel");
            }
        }
        return null;
    }

    private Map<String, String> splitQuery(String query) {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }
}
