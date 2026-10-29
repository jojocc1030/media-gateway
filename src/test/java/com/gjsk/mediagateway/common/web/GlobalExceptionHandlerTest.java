package com.gjsk.mediagateway.common.web;

import com.gjsk.mediagateway.common.api.BaseResponse;
import com.gjsk.mediagateway.common.error.BadRequestException;
import com.gjsk.mediagateway.common.error.ConflictException;
import com.gjsk.mediagateway.common.error.ForbiddenException;
import com.gjsk.mediagateway.common.error.NotFoundException;
import com.gjsk.mediagateway.common.error.UnauthorizedException;
import com.gjsk.mediagateway.support.MvcTestSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MvcTestSupport.standaloneMockMvc(new ThrowingController());

    @Test
    void businessExceptionShouldMapToItsOwnStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseResponse.BAD_REQUEST));

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(BaseResponse.NOT_FOUND));

        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(BaseResponse.FORBIDDEN));

        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(BaseResponse.UNAUTHORIZED));

        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(BaseResponse.CONFLICT));
    }

    @Test
    void validationFailureShouldReturnFieldMessage() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseResponse.BAD_REQUEST))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));
    }

    @Test
    void unreadableJsonShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseResponse.BAD_REQUEST));
    }

    @Test
    void unexpectedExceptionShouldReturnInternalErrorWithoutDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(BaseResponse.INTERNAL_ERROR))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    @Test
    void unsupportedMethodShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/bad-request"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/bad-request")
        public void badRequest() {
            throw new BadRequestException("参数错误");
        }

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new NotFoundException("资源不存在");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new ForbiddenException("权限不足");
        }

        @GetMapping("/test/unauthorized")
        public void unauthorized() {
            throw new UnauthorizedException("未认证");
        }

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new ConflictException("重复请求");
        }

        @GetMapping("/test/boom")
        public void boom() {
            throw new IllegalStateException("internal detail");
        }

        @PostMapping("/test/valid")
        public String valid(@Valid @RequestBody ValidRequest request) {
            return request.name();
        }
    }

    record ValidRequest(@NotBlank(message = "name 不能为空") String name) {
    }
}
