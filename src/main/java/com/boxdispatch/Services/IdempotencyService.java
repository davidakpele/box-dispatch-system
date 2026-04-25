package com.boxdispatch.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.boxdispatch.Exceptions.IdempotencyConflictException;
import com.boxdispatch.Interface.IIdempotencyService;
import com.boxdispatch.Responses.LoadItemsResponse;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService implements IIdempotencyService{

    private static final String RESP_PREFIX = "idempotency:load:resp:";
    private static final String LOCK_PREFIX = "idempotency:load:lock:";
    private static final String LOCK_VALUE  = "1";

    private final RedisTemplate<String, LoadItemsResponse> idempotencyRedisTemplate;
    private final StringRedisTemplate lockRedisTemplate;

    @Value("${app.idempotency.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.idempotency.lock-ttl-seconds:30}")
    private long lockTtlSeconds;

    /**
     * Attempts to claim the idempotency slot.
     *
     * Returns the cached response if the operation already completed.
     * Returns empty if the lock was acquired — caller must execute and call {@link #commit}.
     * Throws {@link IdempotencyConflictException} if another thread holds the lock.
     */
    @Override
    public Optional<LoadItemsResponse> acquire(String key) {
        LoadItemsResponse cached = idempotencyRedisTemplate.opsForValue().get(RESP_PREFIX + key);
        if (cached != null) {
            cached.setIdempotent(true);
            return Optional.of(cached);
        }

        boolean locked = Boolean.TRUE.equals(
                lockRedisTemplate.opsForValue()
                        .setIfAbsent(LOCK_PREFIX + key, LOCK_VALUE, Duration.ofSeconds(lockTtlSeconds))
        );

        if (!locked) {
            throw new IdempotencyConflictException(key);
        }

        return Optional.empty();
    }

    /**
     * Atomically stores the response and releases the lock.
     * Must always be called after {@link #acquire} returns empty — even on failure
     * (pass null to release the lock without caching a response).
     */
    @Override
    public void commit(String key, LoadItemsResponse response) {
        if (response != null) {
            idempotencyRedisTemplate.opsForValue()
                    .set(RESP_PREFIX + key, response, Duration.ofHours(ttlHours));
        }
        lockRedisTemplate.delete(LOCK_PREFIX + key);
    }
}