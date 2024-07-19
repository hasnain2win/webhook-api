

package com.hasnain.webhook_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {

     @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        // Serializer for keys (Strings)
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Serializer for values (Objects)
        GenericToStringSerializer<Object> objectSerializer = new GenericToStringSerializer<>(Object.class);

        // Create serialization context with key and value serializers
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext.<String, Object>newSerializationContext()
                .key(stringRedisSerializer)
                .value(objectSerializer)
                .hashKey(stringRedisSerializer)
                .hashValue(objectSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
    @Bean
    public ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }
}