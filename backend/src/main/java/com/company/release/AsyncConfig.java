package com.company.release;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 通知异步分发（ADR-009）：发送不阻塞发布/报警主流程。 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
