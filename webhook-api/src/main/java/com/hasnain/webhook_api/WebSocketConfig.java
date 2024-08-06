package com.membermismatch.contact.config;

import com.membermismatch.contact.service.ContactSummaryWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private ContactSummaryWebSocketHandler contactSummaryWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        logger.info("Starting registerWebSocketHandlers method: WebSocketConfig");
        registry.addHandler(contactSummaryWebSocketHandler, "/contactSummaryStream/callId/{callId}/agentId/{agentId}/profileType/{profileType}")
                .setAllowedOrigins("*");
        logger.info("Ending registerWebSocketHandlers method: WebSocketConfig");
    }
}
