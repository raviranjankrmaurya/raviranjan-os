package com.raviranjan.os.service.security;

import com.raviranjan.os.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atomic Rolling-Window Contact Form Rate Limiter.
 * Default business rule: Maximum of 3 contact form submissions per client IP during a rolling 10-minute (600s) window.
 *
 * Uses an atomic Redis Lua script on sorted sets (ZREMRANGEBYSCORE + ZCARD + ZADD + PEXPIRE) when Redis is connected.
 * Fallback policy during Redis outages: falls back to an atomic thread-safe in-memory per-IP sliding window
 * (ConcurrentHashMap + synchronized Deque) so rate limiting remains enforced without failing legitimate requests.
 */
@Service
public class ContactRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ContactRateLimiter.class);

    private static final String LUA_SLIDING_WINDOW_SCRIPT =
            "local key = KEYS[1]\n" +
            "local now = tonumber(ARGV[1])\n" +
            "local windowMs = tonumber(ARGV[2])\n" +
            "local maxReqs = tonumber(ARGV[3])\n" +
            "local member = ARGV[4]\n" +
            "local windowStart = now - windowMs\n" +
            "redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)\n" +
            "local currentCount = redis.call('ZCARD', key)\n" +
            "if currentCount >= maxReqs then\n" +
            "    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')\n" +
            "    local retryMs = windowMs\n" +
            "    if oldest ~= nil and #oldest >= 2 then\n" +
            "        retryMs = (tonumber(oldest[2]) + windowMs) - now\n" +
            "    end\n" +
            "    if retryMs < 1000 then retryMs = 1000 end\n" +
            "    return {0, math.ceil(retryMs / 1000), currentCount}\n" +
            "end\n" +
            "redis.call('ZADD', key, now, member)\n" +
            "redis.call('PEXPIRE', key, windowMs)\n" +
            "return {1, 0, currentCount + 1}\n";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> redisScript;
    private final Map<String, Deque<Long>> fallbackBuckets = new ConcurrentHashMap<>();

    private int maxSubmissions;
    private long windowSeconds;
    private Clock clock = Clock.systemUTC();

    @Autowired
    public ContactRateLimiter(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${app.contact.rate-limit.max-submissions:3}") int maxSubmissions,
            @Value("${app.contact.rate-limit.window-seconds:600}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.maxSubmissions = maxSubmissions;
        this.windowSeconds = windowSeconds;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(LUA_SLIDING_WINDOW_SCRIPT);
        this.redisScript.setResultType(List.class);
    }

    /**
     * Checks and atomically records a submission for the given client IP.
     * Returns the current submission count within the rolling window (1..maxSubmissions),
     * or throws RateLimitExceededException (HTTP 429) if the limit is exceeded.
     */
    public int checkAndIncrement(String clientIp) {
        String keyIp = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
        long nowMs = clock.millis();
        long windowMs = windowSeconds * 1000L;

        if (redisTemplate != null) {
            try {
                String redisKey = "ratelimit:contact:ip:" + keyIp;
                String uniqueMember = nowMs + ":" + UUID.randomUUID();
                @SuppressWarnings("unchecked")
                List<Object> result = redisTemplate.execute(
                        redisScript,
                        Collections.singletonList(redisKey),
                        String.valueOf(nowMs),
                        String.valueOf(windowMs),
                        String.valueOf(maxSubmissions),
                        uniqueMember
                );
                if (result != null && result.size() >= 3) {
                    long allowed = ((Number) result.get(0)).longValue();
                    long retryAfterSec = ((Number) result.get(1)).longValue();
                    int count = ((Number) result.get(2)).intValue();
                    if (allowed == 0L) {
                        throw new RateLimitExceededException(
                                String.format("Rate limit exceeded: maximum %d contact submissions allowed per %d minutes. Please try again in %d seconds.",
                                        maxSubmissions, windowSeconds / 60, retryAfterSec),
                                retryAfterSec
                        );
                    }
                    return count;
                }
            } catch (RateLimitExceededException rle) {
                throw rle;
            } catch (Exception redisEx) {
                log.warn("Redis unavailable for contact rate limiting ({}); engaging thread-safe in-memory sliding window fallback.", redisEx.getMessage());
            }
        }

        return checkAndIncrementInMemory(keyIp, nowMs, windowMs);
    }

    private int checkAndIncrementInMemory(String keyIp, long nowMs, long windowMs) {
        Deque<Long> deque = fallbackBuckets.computeIfAbsent(keyIp, k -> new ArrayDeque<>());
        synchronized (deque) {
            long windowStart = nowMs - windowMs;
            while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                deque.pollFirst();
            }
            if (deque.size() >= maxSubmissions) {
                long oldest = deque.peekFirst() != null ? deque.peekFirst() : nowMs;
                long retryMs = Math.max(1000L, (oldest + windowMs) - nowMs);
                long retryAfterSec = (long) Math.ceil(retryMs / 1000.0);
                throw new RateLimitExceededException(
                        String.format("Rate limit exceeded: maximum %d contact submissions allowed per %d minutes. Please try again in %d seconds.",
                                maxSubmissions, windowSeconds / 60, retryAfterSec),
                        retryAfterSec
                );
            }
            deque.addLast(nowMs);
            return deque.size();
        }
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void resetAllForTesting() {
        fallbackBuckets.clear();
    }

    public int getMaxSubmissions() {
        return maxSubmissions;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
