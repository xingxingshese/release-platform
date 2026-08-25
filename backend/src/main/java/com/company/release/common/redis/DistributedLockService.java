package com.company.release.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

import com.company.release.common.exception.ConflictException;

/**
 * Redis 分布式锁（agent.md §二十三）。
 * 加锁 SET NX PX；解锁 Lua 原子校验持有者，防止误删他人锁。
 */
@Service
public class DistributedLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    else
                      return 0
                    end
                    """, Long.class);

    private final StringRedisTemplate redis;

    public DistributedLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 尝试加锁：成功返回 true；已持有返回 false。 */
    public boolean tryLock(String key, String owner, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, ttl));
    }

    /** 加锁，失败抛 CONFLICT（用于"任务已在运行"场景）。 */
    public boolean lockOrThrow(String key, String owner, Duration ttl) {
        if (!tryLock(key, owner, ttl)) {
            throw new ConflictException("operation already running: " + key);
        }
        return true;
    }

    /** 仅持有者可释放（Lua 原子操作）。 */
    public void unlock(String key, String owner) {
        redis.execute(UNLOCK_SCRIPT, List.of(key), owner);
    }

    /** 执行受锁保护的操作模板。 */
    public <T> T withLock(String key, String owner, Duration ttl, LockAction<T> action) {
        lockOrThrow(key, owner, ttl);
        try {
            return action.run();
        } finally {
            unlock(key, owner);
        }
    }

    @FunctionalInterface
    public interface LockAction<T> {
        T run();
    }
}
