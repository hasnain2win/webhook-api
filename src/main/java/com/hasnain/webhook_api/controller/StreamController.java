package com.hasnain.webhook_api.controller;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class StreamController implements MessageListener {

    private final CopyOnWriteArrayList<FluxSink<String>> subscribers = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/webapp/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamEvents(@RequestParam String callerId, @RequestParam String agentId) {
        String channel = "summary:" + callerId + ":" + agentId;
        return Flux.create(emitter -> {
            subscribers.add(emitter);
            emitter.onDispose(() -> subscribers.remove(emitter));
        });
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        for (FluxSink<String> subscriber : subscribers) {
            subscriber.next(payload);
        }
    }
}

