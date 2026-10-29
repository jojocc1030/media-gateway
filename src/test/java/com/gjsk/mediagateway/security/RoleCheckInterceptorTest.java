package com.gjsk.mediagateway.security;

import com.gjsk.mediagateway.common.error.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleCheckInterceptorTest {

    private final RoleCheckInterceptor interceptor = new RoleCheckInterceptor();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/items");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void annotatedMethodShouldThrowForbiddenWithoutRole() throws Exception {
        HandlerMethod handler = handler("adminOnly");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void annotatedMethodShouldPassWithMatchingRole() throws Exception {
        loginWithRoles("ADMIN");
        HandlerMethod handler = handler("adminOnly");

        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    }

    @Test
    void annotatedMethodShouldPassWithAnyMatchingRole() throws Exception {
        loginWithRoles("OPERATOR");
        HandlerMethod handler = handler("adminOrOperator");

        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    }

    @Test
    void unannotatedMethodShouldPassWithoutRole() throws Exception {
        HandlerMethod handler = handler("open");

        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    }

    @Test
    void classLevelAnnotationShouldBeApplied() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ViewerController(),
                ViewerController.class.getMethod("readOnly"));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(ForbiddenException.class);
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
    }

    private void loginWithRoles(String... roles) {
        SessionInfo session = new SessionInfo();
        session.setUserId("user001");
        session.setUsername("admin");
        session.setRoles(Set.of(roles));
        AuthContext.set(session);
    }

    static class TestController {

        @RequireRole({RoleType.ROLE_ADMIN})
        public void adminOnly() {
        }

        @RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})
        public void adminOrOperator() {
        }

        public void open() {
        }
    }

    @RequireRole({RoleType.ROLE_ADMIN})
    static class ViewerController {

        public void readOnly() {
        }
    }
}
