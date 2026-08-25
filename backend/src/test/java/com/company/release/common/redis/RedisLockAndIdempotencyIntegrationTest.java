package com.company.release.common.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADR-010：分布式锁与 Idempotency-Key 幂等（Redis 实现）集成测试。
 */
@Testcontainers
class RedisLockAndIdempotencyIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static StringRedisTemplate redis;
    static DistributedLockService lockService;
    static IdempotencyService idempotencyService;

    @BeforeAll
    static void setUp() {
        var factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        lockService = new DistributedLockService(redis);
        idempotencyService = new IdempotencyService(redis);
    }

    @AfterAll
    static void tearDown() {
        // 容器由 @Container 自动停止
    }

    @Test
    void lockAcquireAndReleaseByOwner() {
        String key = "release:lock:t1";
        assertThat(lockService.tryLock(key, "owner-a", Duration.ofSeconds(10))).isTrue();
        assertThat(lockService.tryLock(key, "owner-b", Duration.ofSeconds(10))).isFalse();
        lockService.unlock(key, "owner-a");
        assertThat(lockService.tryLock(key, "owner-b", Duration.ofSeconds(10))).isTrue();
    }

    @Test
    void cannotUnlockWithWrongOwner() {
        String key = "release:lock:t2";
        assertThat(lockService.tryLock(key, "owner-a", Duration.ofSeconds(10))).isTrue();
        lockService.unlock(key, "owner-wrong"); // 必须无效果（Lua 原子校验）
        assertThat(redis.hasKey(key)).isTrue();
        lockService.unlock(key, "owner-a");
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void lockExpiresAutomatically() throws InterruptedException {
        String key = "release:lock:t3";
        assertThat(lockService.tryLock(key, "owner-a", Duration.ofMillis(200))).isTrue();
        Thread.sleep(800); // 留足过期余量，避免 CI 抖动
        assertThat(lockService.tryLock(key, "owner-b", Duration.ofSeconds(5))).isTrue();
    }

    @Test
    void duplicateOperationRejected() {
        String key = "idem:accept:t1";
        assertThat(idempotencyService.putIfAbsent(key, "{\"result\":\"accepted\"}", Duration.ofMinutes(10)))
                .isNull();
        // 第二次同 key：返回首次结果，实现幂等
        assertThat(idempotencyService.putIfAbsent(key, "{\"result\":\"accepted\"}", Duration.ofMinutes(10)))
                .isEqualTo("{\"result\":\"accepted\"}");
    }

    @Test
    void differentKeysAreIndependent() {
        String k1 = "idem:x:a", k2 = "idem:x:b";
        assertThat(idempotencyService.putIfAbsent(k1, "1", Duration.ofSeconds(60))).isNull();
        assertThat(idempotencyService.putIfAbsent(k2, "2", Duration.ofSeconds(60))).isNull();
    }

    @Test
    void duplicateLockIsConflict() {
        String key = "release:lock:t4";
        assertThat(lockService.lockOrThrow(key, "owner-a", Duration.ofSeconds(10))).isTrue();
        assertThatThrownBy(() -> lockService.lockOrThrow(key, "owner-b", Duration.ofSeconds(10)))
                .isInstanceOf(com.company.release.common.exception.ConflictException.class)
                .hasMessageContaining("already running");
    }
}
