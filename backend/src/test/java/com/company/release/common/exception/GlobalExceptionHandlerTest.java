package com.company.release.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 规范 §28：统一错误模型 {code,message,requestId,details}；
 * 禁止向前端返回 Java StackTrace。
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"})
@Import(GlobalExceptionHandlerTest.DummyController.class)
class GlobalExceptionHandlerTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    com.company.release.iam.auth.AuthService authService;

    @org.springframework.boot.test.mock.mockito.MockBean
    com.company.release.iam.auth.JwtService jwtService;

    @org.springframework.boot.test.mock.mockito.MockBean
    com.company.release.project.application.ProjectService projectService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessExceptionMapsToItsErrorCode() throws Exception {
        mockMvc.perform(get("/dummy/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("plan not editable"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void notFoundExceptionMapsToNotFound() throws Exception {
        mockMvc.perform(get("/dummy/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void illegalStateTransitionIsConflict() throws Exception {
        mockMvc.perform(get("/dummy/state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("TEST_MERGING")));
    }

    @Test
    void validationErrorHandled() throws Exception {
        MvcResult result = mockMvc.perform(get("/dummy/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("at java.");
    }

    @Test
    void unknownExceptionBecomesSystemErrorWithoutStackTrace() throws Exception {
        MvcResult result = mockMvc.perform(get("/dummy/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SYSTEM_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("java.lang");
    }

    @TestConfiguration
    static class Cfg {
    }

    @RestController
    static class DummyController {

        @GetMapping("/dummy/business")
        public String business() {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "plan not editable");
        }

        @GetMapping("/dummy/not-found")
        public String notFound() {
            throw new NotFoundException("release plan", "10086");
        }

        @GetMapping("/dummy/state")
        public String state() {
            throw new com.company.release.release.domain.state.IllegalStateTransitionException(
                    com.company.release.release.domain.state.ReleaseStatus.TEST_MERGING,
                    com.company.release.release.domain.state.ReleaseStatus.WAIT_TEST_ACCEPT);
        }

        @GetMapping("/dummy/validation")
        public String validation() {
            throw new IllegalArgumentException("name must not be blank");
        }

        @GetMapping("/dummy/boom")
        public String boom() {
            throw new RuntimeException("unexpected NPE inside");
        }
    }

}
