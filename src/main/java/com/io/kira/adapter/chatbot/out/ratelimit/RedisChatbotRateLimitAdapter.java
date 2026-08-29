package com.io.kira.adapter.chatbot.out.ratelimit;

import com.io.kira.application.chatbot.port.out.ChatbotRateLimitPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RedisChatbotRateLimitAdapter implements ChatbotRateLimitPort {

    private static final Logger log = LoggerFactory.getLogger(RedisChatbotRateLimitAdapter.class);
    private static final long WINDOW_SECONDS = 60;
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;

    public RedisChatbotRateLimitAdapter(
            StringRedisTemplate redisTemplate,
            @Value("${chatbot.rate-limit.requests-per-minute:12}") int requestsPerMinute
    ) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
    }

    @Override
    public boolean tryAcquire(UUID userId) {
        if (userId == null) {
            return false;
        }

        String key = "codetracker:echo:rate:" + userId;
        try {
            Long count = redisTemplate.execute(
                    INCREMENT_WITH_EXPIRY,
                    List.of(key),
                    Long.toString(WINDOW_SECONDS)
            );
            return count == null || count <= requestsPerMinute;
        } catch (RuntimeException exception) {
            // Echo should remain usable during a temporary Redis outage.
            log.warn("Echo rate limiter unavailable; allowing request: {}",
                    exception.getClass().getSimpleName());
            return true;
        }
    }
}
