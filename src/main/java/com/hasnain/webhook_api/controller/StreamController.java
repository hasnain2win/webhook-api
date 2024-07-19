package com.hasnain.webhook_api.controller;

import com.hasnain.webhook_api.StreamRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
public class StreamController {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    private ReactiveRedisMessageListenerContainer container;

    @GetMapping(value = "/webapp/api/stream", produces = "text/event-stream")
    public SseEmitter streamEvents(@RequestBody StreamRequest streamRequest) {
        String channel = String.format("summary-creator:%s", streamRequest.getAgentId());
        System.out.println("Subscribing to channel " + channel);
       /* // Create a Flux that listens to the Redis channel
        Flux<String> messageFlux = container.receive(ChannelTopic.of(channel))
                .map(message -> new String(message.getMessage()));

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30-minute timeout

        // Send data to the client
        messageFlux.subscribe(
                message -> {
                    try {
                        emitter.send(SseEmitter.event().data(message));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> emitter.completeWithError(error),
                () -> emitter.complete()
        );

        return emitter;
    }*/
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes

        long startTime = System.currentTimeMillis();

        Flux<? extends ReactiveSubscription.Message<String, String>> messageFlux = reactiveRedisTemplate.listenToChannel(channel)
                .doOnSubscribe(subscription -> System.out.println("Subscribed to channel: " + channel))
                .doOnNext(message -> {
                    try {
                        emitter.send(SseEmitter.event().data(message));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                    System.out.println("Message sent at: " + (System.currentTimeMillis() - startTime) + " ms");
                })
                .doOnError(error -> {
                    System.err.println("Error receiving message: " + error.getMessage());
                    emitter.completeWithError(error);
                })
                .doOnComplete(() -> System.out.println("Subscription completed"));

        messageFlux.subscribe();

        emitter.onCompletion(() -> emitter.complete());
        emitter.onError(e -> emitter.completeWithError(e));

        return emitter;
    }
}

