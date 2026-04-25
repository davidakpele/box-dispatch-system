package com.boxdispatch.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.boxdispatch.Responses.LoadItemsResponse;


@Configuration
public class RedisConfig {

    @SuppressWarnings("deprecation")
    @Bean
    public RedisTemplate<String, LoadItemsResponse> idempotencyRedisTemplate(
            RedisConnectionFactory factory, ObjectMapper objectMapper) {

        var template = new RedisTemplate<String, LoadItemsResponse>();
        template.setConnectionFactory(factory);

        var jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, LoadItemsResponse.class);
        var stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public StringRedisTemplate lockRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}