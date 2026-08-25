package com.company.release.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Idempotency-Key 幂等（ADR-010）：
 * putIfAbsent 返回 null 表示首次执行；返回非 null 表示重复请求，直接返回首次结果。
 * Lua 保证"读取旧值 + 写入"原子性。
 */
@Service
public class IdempotencyService {

    private static final DefaultRedisScript<String> PUT_IF_ABSENT = new DefaultRedisScript<>(
            """
                    local v = redis.call('GET', KEYS[1])
                    if v then
                      return v
                    end
                    redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                    return nil
                    """, String.class);

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @return null = 首次（调用方继续执行）；
     *         非 null = 重复请求，值为首次执行的响应快照。
     */
    public String putIfAbsent(String key, String resultSnapshot, Duration ttl) {
        return redis.execute(PUT_IF_ABSENT, List.of(key), resultSnapshot,
                String.valueOf(ttl.toMillis()));
    }

    /** 首次执行完成后回写结果（供"先占位后执行"模式使用）。 */
    public void storeResult(String key, String resultSnapshot, Duration ttl) {
        redis.opsForValue().set(key, resultSnapshot, ttl);
    }

    public String getResult(String key) {
        return redis.opsForValue().get(key);
    }
}
