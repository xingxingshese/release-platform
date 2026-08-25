package com.company.release.common.web;

import org.junit.jupiter.api.Test;
import com.company.release.common.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 规范 §26：日志必须包含 requestId；响应头回传 X-Request-ID。
 */
@WebMvcTest(controllers = RequestIdFilterTest.EchoController.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"})
@Import({RequestIdFilter.class, GlobalExceptionHandler.class, RequestIdFilterTest.EchoController.class})
class RequestIdFilterTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    com.company.release.iam.auth.AuthService authService;

    @org.springframework.boot.test.mock.mockito.MockBean
    com.company.release.iam.auth.JwtService jwtService;

    @org.springframework.boot.test.mock.mockito.MockBean
    com.company.release.project.application.ProjectService projectService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatesRequestIdWhenAbsent() throws Exception {
        MvcResult r = mockMvc.perform(get("/echo"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getHeader("X-Request-ID")).isNotBlank();
    }

    @Test
    void reusesIncomingRequestId() throws Exception {
        MvcResult r = mockMvc.perform(get("/echo").header("X-Request-ID", "req-123"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getHeader("X-Request-ID")).isEqualTo("req-123");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class Cfg {
    }

    @org.springframework.web.bind.annotation.RestController
    static class EchoController {
        @org.springframework.web.bind.annotation.GetMapping("/echo")
        public String echo(@org.springframework.lang.NonNull jakarta.servlet.http.HttpServletRequest request) {
            return org.slf4j.MDC.get("requestId");
        }
    }
}
