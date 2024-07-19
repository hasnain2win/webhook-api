package com.hasnain.webhook_api.controller;

import com.hasnain.webhook_api.StreamRequest;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class StreamController implements MessageListener {


    private final CopyOnWriteArrayList<FluxSink<String>> subscribers = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/webapp/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamEvents(@RequestBody StreamRequest streamRequest) {
        String channel = streamRequest.getCallerId() + ":" + streamRequest.getAgentId();
        System.out.println("Subscribing to channel: " + channel);

        return Flux.<String>create(emitter -> {
            subscribers.add(emitter);
            emitter.onDispose(() -> {
                subscribers.remove(emitter);
                System.out.println("Unsubscribed from channel: " + channel);
            });
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        System.out.println("Received message: " + payload);

        for (FluxSink<String> subscriber : subscribers) {
            subscriber.next(payload);
        }
    }
}
